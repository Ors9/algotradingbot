package com.algotradingbot.engine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.algotradingbot.core.Candle;
import com.ib.client.Bar;
import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDescription;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.DepthMktDataDescription;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.FamilyCode;
import com.ib.client.HistogramEntry;
import com.ib.client.HistoricalSession;
import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;
import com.ib.client.NewsProvider;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.PriceIncrement;
import com.ib.client.SoftDollarTier;
import com.ib.client.TickAttrib;
import com.ib.client.TickAttribBidAsk;
import com.ib.client.TickAttribLast;

public class GetDataFromInteractiveBroker implements EWrapper {

    // פורמט תאריך IB ידני (כשמעבירים endDateTime כמחרוזת)
    private static final java.time.format.DateTimeFormatter IB_FMT
            = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss").withZone(java.time.ZoneOffset.UTC);

    // מטרה: מתי לעצור (epoch seconds) לפי "5 Y"
    private long targetStartEpoch = Long.MAX_VALUE;

    // לולאת איסוף: end דינמי שיתקדם לאחור
    private volatile String pagingEndDateTime = "";

    // מעקב אחרי הנר הכי מוקדם בכל מנה
    private volatile long earliestEpochInBatch = Long.MAX_VALUE;

    // מזהה בקשה פנימי (לא להשתמש ב-nextOrderId)
    private int histReqId = 5000;

    // לניהול המתנה לכל מנה
    private java.util.concurrent.CountDownLatch batchDone;

    // דגל ריצה + לאצ' סופי שמסמן ש"הורדתי הכל"
    private volatile boolean rangeRunnerStarted = false;
    private final java.util.concurrent.CountDownLatch allDone = new java.util.concurrent.CountDownLatch(1);
    // AFTER:
    private static final String BATCH_DURATION = "1 Y";  // נסה קודם 1Y, ואם יקפוץ גודל-נתונים רד ל-"6M"

    private final String currency;
    private final String timeFrame;
    private final String duration;
    private final String endDateTime;
    private final String whatToShow;
    private final boolean useRTH;
    private final int port;
    private final String ip;

    private final ArrayList<Candle> candles;

    private EClientSocket client;
    private EJavaSignal signal;
    private EReader reader;

    private int nextOrderId = -1;     // מקבל ערך מ-nextValidId

    public GetDataFromInteractiveBroker(String currency, String timeFrame, String duration, String endDateTime,
            String whatToShow, boolean useRTH,
            int port, String ip) {
        this.currency = currency;
        this.timeFrame = timeFrame;
        this.duration = duration;
        this.endDateTime = endDateTime;
        this.whatToShow = whatToShow;
        this.useRTH = useRTH;
        this.port = port;
        this.ip = ip;
        this.candles = new ArrayList<>();
    }

    public void connectToInteractiveBroker() {
        System.out.println("🚀 Attempting connection to IB on IP: " + ip + " Port: " + port);

        signal = new EJavaSignal();

        client = new EClientSocket(this, signal);

        client.eConnect(ip, port, 1);
        System.out.printf("Attempted connect to %s:%d%n", ip, port);
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }
        if (!client.isConnected()) {
            System.err.println("Still not connected after eConnect (TCP refused/blocked or wrong IP/port/API settings).");
            return;
        }

        reader = new EReader(client, signal);
        reader.start();

        new Thread(() -> {
            while (client.isConnected()) {
                signal.waitForSignal();
                try {
                    reader.processMsgs();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void historicalData(int reqId, Bar bar) {
        // With formatDate=2, bar.time() is epoch seconds (often as a String)
        long epochSec;
        try {
            epochSec = Long.parseLong(bar.time());
        } catch (NumberFormatException e) {
            // rare fallback if IB returns a date string here
            epochSec = com.algotradingbot.utils.TimeUtils.parseIb(bar.time())
                    .atZone(java.time.ZoneId.systemDefault())
                    .toEpochSecond();
        }

        if (epochSec < earliestEpochInBatch) {
            earliestEpochInBatch = epochSec;
        }

        double volume = Double.parseDouble(bar.volume().toString());

        // Use the “any” helper to get your legacy string
        String dateStr = com.algotradingbot.utils.TimeUtils.epochToLegacyString(epochSec);
        candles.add(new Candle(
                dateStr,
                bar.open(), bar.high(), bar.low(), bar.close(), volume
        ));
    }

    @Override
    public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
        System.out.println("Historical data batch end. From: " + startDateStr + " To: " + endDateStr);
        if (batchDone != null) {
            batchDone.countDown();
        }
    }

    public void awaitHistoricalData() {
        try {
            allDone.await();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void nextValidId(int orderId) {
        this.nextOrderId = orderId; // רק סימון זמינות
        maybeStartRangeRunner();
    }

    private void maybeStartRangeRunner() {
        if (!client.isConnected() || nextOrderId == -1 || rangeRunnerStarted) {
            return;
        }
        rangeRunnerStarted = true;

        // גוזרים יעד התחלה מתוך duration ("5 Y") יחסית ל-endDateTime (ריק=עכשיו)
        long endEpoch = endDateTime == null || endDateTime.isEmpty()
                ? java.time.Instant.now().getEpochSecond()
                : parseIbDateToEpoch(endDateTime);
        long spanSec = parseDurationToSeconds(duration); // "5 Y" -> שניות
        targetStartEpoch = endEpoch - spanSec;

        // נקודת ה-end הראשונה: או "" (=עכשיו) או מה שנתת
        pagingEndDateTime = (endDateTime == null) ? "" : endDateTime;

        // מריצים לולאת פריסה במיתר נפרד
        new Thread(this::runPagedHistoryLoop, "IB-History-RangeRunner").start();
    }

    private void runPagedHistoryLoop() {
        try {
            while (true) {
                // נכין לאצ' למנה הנוכחית
                batchDone = new java.util.concurrent.CountDownLatch(1);
                earliestEpochInBatch = Long.MAX_VALUE;

                // שולחים בקשה: 1 חודש אחורה מה-end הנוכחי, ב-barSize שקיבלת מבחוץ ("1 hour")
                int reqId = histReqId++;
                Contract contract = buildFxContract(currency);

                client.reqHistoricalData(
                        reqId, contract, pagingEndDateTime,
                        BATCH_DURATION, timeFrame, whatToShow,
                        useRTH ? 1 : 0,
                        2, // epoch seconds
                        false, null
                );

                // המתנה לסיום המנה (historicalDataEnd)
                batchDone.await();

                // אם לא קיבלנו כלום – אין כבר נתונים
                if (earliestEpochInBatch == Long.MAX_VALUE) {
                    break;
                }

                // עצירת תנאי: הגענו/עברנו את יעד ההתחלה (לפני 5 שנים)
                if (earliestEpochInBatch <= targetStartEpoch) {
                    break;
                }

                // end הבא = שנייה לפני הנר הכי מוקדם במנה הזו
                long nextEndEpoch = earliestEpochInBatch - 1;
                pagingEndDateTime = fmtEndUtc(nextEndEpoch);

                // מנוחה קטנה נגד pacing
                try {
                    Thread.sleep(1100);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (InterruptedException ignored) {
            // אפשר לוג
        } finally {
            allDone.countDown(); // מסמן ל-runSinglePeriodTest שאפשר להמשיך
        }
    }

    private static final Set<String> FIAT = Set.of(
            "USD", "EUR", "GBP", "JPY", "CHF", "AUD", "CAD", "NZD", "SEK", "NOK", "DKK", "HKD", "SGD", "ILS", "ZAR", "MXN", "CNY"
    );

    private Contract buildFxContract(String pair) {
        String s = pair.replace("/", "").trim().toUpperCase();

        if (s.length() != 6) {
            throw new IllegalArgumentException("Forex pair must be 6 letters, e.g., EURUSD");
        }

        String base = s.substring(0, 3);
        String quote = s.substring(3, 6);

        if (!FIAT.contains(base) || !FIAT.contains(quote)) {
            throw new IllegalArgumentException("Unsupported forex pair: " + pair);
        }

        Contract c = new Contract();
        c.symbol(base);
        c.secType("CASH");
        c.currency(quote);
        c.exchange("IDEALPRO");
        return c;
    }

    private long parseIbDateToEpoch(String s) {
        s = s.trim().replace('-', ' ');

        // Case: "yyyyMMdd" only
        if (s.length() == 8) {
            s += " 00:00:00";
        }

        // With zone: "yyyyMMdd HH:mm:ss ZZZ"
        String[] parts = s.split("\\s+");
        if (parts.length >= 3) {
            String ymdHms = parts[0] + " " + parts[1];
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(
                    ymdHms,
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
            );
            java.time.ZoneId zone = java.time.ZoneId.of(parts[2]);
            return ldt.atZone(zone).toEpochSecond();
        }

        // Without zone: assume UTC
        java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(
                s,
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
        );
        return ldt.toEpochSecond(java.time.ZoneOffset.UTC);
    }

    // אופציה A: UTC (מומלץ)
    private static String fmtEndUtc(long epochSec) {
        return IB_FMT.format(Instant.ofEpochSecond(epochSec)) + " UTC";
    }

// עוזר: "5 Y" / "12 M" / "30 D" → שניות (ראף, מספיק לצורך עצירה)
    private long parseDurationToSeconds(String dur) {
        if (dur == null || dur.isEmpty()) {
            return 0;
        }
        String[] parts = dur.trim().split("\\s+");
        if (parts.length != 2) {
            return 0;
        }
        long n = Long.parseLong(parts[0]);
        String unit = parts[1].toUpperCase();
        switch (unit) {
            case "Y":
                return n * 365L * 24 * 3600;
            case "M":
                return n * 30L * 24 * 3600;
            case "W":
                return n * 7L * 24 * 3600;
            case "D":
                return n * 24L * 3600;
            case "H":
                return n * 3600L;
            default:
                return 0;
        }
    }

    @Override
    public void error(Exception e) {
        System.err.println("Exception: " + e.getMessage());
        e.printStackTrace();
    }

    @Override
    public void error(String str) {
        System.err.println("Error: " + str);
    }

    @Override
    public void error(int id, int errorCode, String errorMsg, String advancedInfo) {
        System.err.println("Error. Id: " + id + ", Code: " + errorCode + ", Msg: " + errorMsg + ", AdvInfo: " + advancedInfo);
    }

    public String getCurrency() {
        return currency;
    }

    public String getTimeFrame() {
        return timeFrame;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public ArrayList<Candle> getCandles() {
        return new ArrayList(candles);
    }

    @Override
    public void accountDownloadEnd(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accountDownloadEnd'");
    }

    @Override
    public void accountSummary(int arg0, String arg1, String arg2, String arg3, String arg4) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accountSummary'");
    }

    @Override
    public void accountSummaryEnd(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accountSummaryEnd'");
    }

    @Override
    public void accountUpdateMulti(int arg0, String arg1, String arg2, String arg3, String arg4, String arg5) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accountUpdateMulti'");
    }

    @Override
    public void accountUpdateMultiEnd(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'accountUpdateMultiEnd'");
    }

    @Override
    public void bondContractDetails(int arg0, ContractDetails arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bondContractDetails'");
    }

    @Override
    public void commissionReport(CommissionReport arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'commissionReport'");
    }

    @Override
    public void completedOrder(Contract arg0, Order arg1, OrderState arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'completedOrder'");
    }

    @Override
    public void completedOrdersEnd() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'completedOrdersEnd'");
    }

    @Override
    public void connectAck() {
        System.out.println("Connected (ACK).");
    }

    @Override
    public void connectionClosed() {
        System.out.println("Disconnected from Interactive Brokers.");
    }

    @Override
    public void contractDetails(int arg0, ContractDetails arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'contractDetails'");
    }

    @Override
    public void contractDetailsEnd(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'contractDetailsEnd'");
    }

    @Override
    public void currentTime(long arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'currentTime'");
    }

    @Override
    public void deltaNeutralValidation(int arg0, DeltaNeutralContract arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deltaNeutralValidation'");
    }

    @Override
    public void displayGroupList(int arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'displayGroupList'");
    }

    @Override
    public void displayGroupUpdated(int arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'displayGroupUpdated'");
    }

    @Override
    public void execDetails(int arg0, Contract arg1, Execution arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execDetails'");
    }

    @Override
    public void execDetailsEnd(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'execDetailsEnd'");
    }

    @Override
    public void familyCodes(FamilyCode[] arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'familyCodes'");
    }

    @Override
    public void fundamentalData(int arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fundamentalData'");
    }

    @Override
    public void headTimestamp(int arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'headTimestamp'");
    }

    @Override
    public void histogramData(int arg0, List<HistogramEntry> arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'histogramData'");
    }

    @Override
    public void historicalDataUpdate(int arg0, Bar arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'historicalDataUpdate'");
    }

    @Override
    public void historicalNews(int arg0, String arg1, String arg2, String arg3, String arg4) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'historicalNews'");
    }

    @Override
    public void historicalNewsEnd(int arg0, boolean arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'historicalNewsEnd'");
    }

    @Override
    public void historicalSchedule(int arg0, String arg1, String arg2, String arg3, List<HistoricalSession> arg4) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'historicalSchedule'");
    }

    @Override
    public void historicalTicks(int arg0, List<HistoricalTick> arg1, boolean arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'historicalTicks'");
    }

    @Override
    public void historicalTicksBidAsk(int arg0, List<HistoricalTickBidAsk> arg1, boolean arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'historicalTicksBidAsk'");
    }

    @Override
    public void historicalTicksLast(int arg0, List<HistoricalTickLast> arg1, boolean arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'historicalTicksLast'");
    }

    @Override
    public void managedAccounts(String accountsList) {
        System.out.println("Accounts: " + accountsList);
    }

    @Override
    public void marketDataType(int arg0, int arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'marketDataType'");
    }

    @Override
    public void marketRule(int arg0, PriceIncrement[] arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'marketRule'");
    }

    @Override
    public void mktDepthExchanges(DepthMktDataDescription[] arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'mktDepthExchanges'");
    }

    @Override
    public void newsArticle(int arg0, int arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newsArticle'");
    }

    @Override
    public void newsProviders(NewsProvider[] arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newsProviders'");
    }

    @Override
    public void openOrder(int arg0, Contract arg1, Order arg2, OrderState arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'openOrder'");
    }

    @Override
    public void openOrderEnd() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'openOrderEnd'");
    }

    @Override
    public void orderBound(long arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'orderBound'");
    }

    @Override
    public void orderStatus(int arg0, String arg1, Decimal arg2, Decimal arg3, double arg4, int arg5, int arg6,
            double arg7, int arg8, String arg9, double arg10) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'orderStatus'");
    }

    @Override
    public void pnl(int arg0, double arg1, double arg2, double arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'pnl'");
    }

    @Override
    public void pnlSingle(int arg0, Decimal arg1, double arg2, double arg3, double arg4, double arg5) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'pnlSingle'");
    }

    @Override
    public void position(String arg0, Contract arg1, Decimal arg2, double arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'position'");
    }

    @Override
    public void positionEnd() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'positionEnd'");
    }

    @Override
    public void positionMulti(int arg0, String arg1, String arg2, Contract arg3, Decimal arg4, double arg5) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'positionMulti'");
    }

    @Override
    public void positionMultiEnd(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'positionMultiEnd'");
    }

    @Override
    public void realtimeBar(int arg0, long arg1, double arg2, double arg3, double arg4, double arg5, Decimal arg6,
            Decimal arg7, int arg8) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'realtimeBar'");
    }

    @Override
    public void receiveFA(int arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receiveFA'");
    }

    @Override
    public void replaceFAEnd(int arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'replaceFAEnd'");
    }

    @Override
    public void rerouteMktDataReq(int arg0, int arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'rerouteMktDataReq'");
    }

    @Override
    public void rerouteMktDepthReq(int arg0, int arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'rerouteMktDepthReq'");
    }

    @Override
    public void scannerData(int arg0, int arg1, ContractDetails arg2, String arg3, String arg4, String arg5,
            String arg6) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'scannerData'");
    }

    @Override
    public void scannerDataEnd(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'scannerDataEnd'");
    }

    @Override
    public void scannerParameters(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'scannerParameters'");
    }

    @Override
    public void securityDefinitionOptionalParameter(int arg0, String arg1, int arg2, String arg3, String arg4,
            Set<String> arg5, Set<Double> arg6) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'securityDefinitionOptionalParameter'");
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'securityDefinitionOptionalParameterEnd'");
    }

    @Override
    public void smartComponents(int arg0, Map<Integer, Entry<String, Character>> arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'smartComponents'");
    }

    @Override
    public void softDollarTiers(int arg0, SoftDollarTier[] arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'softDollarTiers'");
    }

    @Override
    public void symbolSamples(int arg0, ContractDescription[] arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'symbolSamples'");
    }

    @Override
    public void tickByTickAllLast(int arg0, int arg1, long arg2, double arg3, Decimal arg4, TickAttribLast arg5,
            String arg6, String arg7) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickByTickAllLast'");
    }

    @Override
    public void tickByTickBidAsk(int arg0, long arg1, double arg2, double arg3, Decimal arg4, Decimal arg5,
            TickAttribBidAsk arg6) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickByTickBidAsk'");
    }

    @Override
    public void tickByTickMidPoint(int arg0, long arg1, double arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickByTickMidPoint'");
    }

    @Override
    public void tickEFP(int arg0, int arg1, double arg2, String arg3, double arg4, int arg5, String arg6, double arg7,
            double arg8) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickEFP'");
    }

    @Override
    public void tickGeneric(int arg0, int arg1, double arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickGeneric'");
    }

    @Override
    public void tickNews(int arg0, long arg1, String arg2, String arg3, String arg4, String arg5) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickNews'");
    }

    @Override
    public void tickOptionComputation(int arg0, int arg1, int arg2, double arg3, double arg4, double arg5, double arg6,
            double arg7, double arg8, double arg9, double arg10) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickOptionComputation'");
    }

    @Override
    public void tickPrice(int arg0, int arg1, double arg2, TickAttrib arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickPrice'");
    }

    @Override
    public void tickReqParams(int arg0, double arg1, String arg2, int arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickReqParams'");
    }

    @Override
    public void tickSize(int arg0, int arg1, Decimal arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickSize'");
    }

    @Override
    public void tickSnapshotEnd(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickSnapshotEnd'");
    }

    @Override
    public void tickString(int arg0, int arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tickString'");
    }

    @Override
    public void updateAccountTime(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateAccountTime'");
    }

    @Override
    public void updateAccountValue(String arg0, String arg1, String arg2, String arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateAccountValue'");
    }

    @Override
    public void updateMktDepth(int arg0, int arg1, int arg2, int arg3, double arg4, Decimal arg5) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateMktDepth'");
    }

    @Override
    public void updateMktDepthL2(int arg0, int arg1, String arg2, int arg3, int arg4, double arg5, Decimal arg6,
            boolean arg7) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateMktDepthL2'");
    }

    @Override
    public void updateNewsBulletin(int arg0, int arg1, String arg2, String arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateNewsBulletin'");
    }

    @Override
    public void updatePortfolio(Contract arg0, Decimal arg1, double arg2, double arg3, double arg4, double arg5,
            double arg6, String arg7) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updatePortfolio'");
    }

    @Override
    public void userInfo(int arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'userInfo'");
    }

    @Override
    public void verifyAndAuthCompleted(boolean arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'verifyAndAuthCompleted'");
    }

    @Override
    public void verifyAndAuthMessageAPI(String arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'verifyAndAuthMessageAPI'");
    }

    @Override
    public void verifyCompleted(boolean arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'verifyCompleted'");
    }

    @Override
    public void verifyMessageAPI(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'verifyMessageAPI'");
    }

    @Override
    public void wshEventData(int arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'wshEventData'");
    }

    @Override
    public void wshMetaData(int arg0, String arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'wshMetaData'");
    }

}
