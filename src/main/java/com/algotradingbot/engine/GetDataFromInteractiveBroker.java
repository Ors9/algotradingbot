package com.algotradingbot.engine;

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


/*שלבי המשך: 
2. שלח בקשה למשיכת נתוני היסטוריה
כתוב פונקציה requestHistoricalData() שקוראת ל־client.reqHistoricalData().

בנה Contract מתאים למטבע/נכס שלך (למשל EURUSD).

שלוט על פרמטרים כמו durationStr, barSizeSetting, whatToShow ו־useRTH.

3. טפל בקבלת נתונים (Callbacks)
מימש את historicalData(int reqId, Bar bar) — כאן אתה מקבל כל נר ומאחסן אותו ברשימה candles.

מימש את historicalDataEnd(int reqId, String start, String end) — זה איתות שהנתונים היסטוריים הושלמו.

במידת הצורך, עדכן UI או העבר את הנתונים הלאה במתודה זו.
 * 
 * 
 * 
 * 
 */
public class GetDataFromInteractiveBroker implements EWrapper {

    private final String currency;
    private final String timeFrame;
    private final int port;
    private final String ip;
    private final ArrayList<Candle> candles;

    private EClientSocket client;
    private EJavaSignal signal;
    private EReader reader;

    public GetDataFromInteractiveBroker(String currency, int port, String timeFrame, String ip) {
        this.currency = currency;
        this.port = port;
        this.timeFrame = timeFrame;
        this.ip = ip;
        candles = new ArrayList<>();
    }

    public void connectToInteractiveBroker() {
        // 1. צור את ה-signal (עוזר ל-EReader)
        signal = new EJavaSignal();

        // 2. צור את ה-client עם המחלקה שמיישמת EWrapper (this)
        client = new EClientSocket(this, signal);

        // 3. חבר את ה-client לכתובת IP והפורט
        client.eConnect(ip, port, 0);  // 0 הוא clientId

        // 4. הפעל את ה-EReader כדי לעבד הודעות נכנסות
        reader = new EReader(client, signal);
        reader.start();

        // 5. הפעל לולאה שקוראת הודעות כל זמן שהחיבור פעיל
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
    public void historicalData(int arg0, Bar arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'historicalData'");
    }

    @Override
    public void historicalDataEnd(int arg0, String arg1, String arg2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'historicalDataEnd'");
    }

    @Override
    public void nextValidId(int arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'nextValidId'");
    }

    @Override
    public void error(Exception arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'error'");
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'connectAck'");
    }

    @Override
    public void connectionClosed() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'connectionClosed'");
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
    public void error(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'error'");
    }

    @Override
    public void error(int arg0, int arg1, String arg2, String arg3) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'error'");
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
    public void managedAccounts(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'managedAccounts'");
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
