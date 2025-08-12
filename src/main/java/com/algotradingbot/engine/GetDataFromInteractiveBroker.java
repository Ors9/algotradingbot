package com.algotradingbot.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.algotradingbot.core.Candle;
import com.algotradingbot.utils.TimeUtils;
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

    private final java.util.concurrent.CountDownLatch histDone = new java.util.concurrent.CountDownLatch(1);

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

    private int requestId = 1001;     // מזהה ייחודי לבקשת היסטוריה
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

    public void requestHistoricalData() {
        if (nextOrderId == -1) {
            System.err.println("❌ Cannot request data — nextOrderId not yet received.");
            return;
        }

        Contract contract = new Contract();
        contract.symbol(currency);         // לדוגמה: "EUR"
        contract.secType("CASH");
        contract.currency("USD");
        contract.exchange("IDEALPRO");

        client.reqHistoricalData(
                nextOrderId, // unique request id
                contract,
                endDateTime, // "" = now
                duration, // e.g. "1 M"
                timeFrame, // e.g. "1 hour"
                whatToShow, // e.g. "MIDPOINT"
                useRTH ? 1 : 0, // 1 = only RTH
                1, // formatDate
                false, // keepUpToDate
                null // chart options
        );
    }

    @Override
    public void historicalData(int reqId, Bar bar) {
        double volume = Double.parseDouble(bar.volume().toString());
        Candle candle = new Candle(
                TimeUtils.ibToLegacyString(bar.time()),
                bar.open(),
                bar.high(),
                bar.low(),
                bar.close(),
                volume
        );

        candles.add(candle);
    }

    @Override
    public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
        System.out.println("Historical data reception ended. From: " + startDateStr + " To: " + endDateStr);
        histDone.countDown();
    }

    public void awaitHistoricalData() {
        try {
            histDone.await();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void nextValidId(int orderId) {
        System.out.println("✅ nextValidId: " + orderId);
        this.nextOrderId = orderId;

        // ברגע שהתחברנו, נבקש נתונים היסטוריים
        requestHistoricalData();
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
        return candles;
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
