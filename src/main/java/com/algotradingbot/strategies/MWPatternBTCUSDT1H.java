package com.algotradingbot.strategies;

import java.util.ArrayList;

import com.algotradingbot.core.Candle;
import com.algotradingbot.core.Signal;
import com.algotradingbot.core.TradingStrategy;
import com.algotradingbot.utils.MPatternTest;
import com.algotradingbot.utils.TrendUtils;
import com.algotradingbot.utils.WPatternBTCUSDT1H;
/*
    BTCUSDT 1H results!
 * === Combined Performance ===
W:73  | L:36  | WinRate: 66.97% | Profit: $  366.62 | MaxDD: $  125.42
 * 
 */
public class MWPatternBTCUSDT1H extends TradingStrategy{
    private static final int START_PERIOD = 70;
    private static final int ATR_PERIOD = 10;
    private static final double RR_RATIO = 0.9;   
    private static final double ATR_MULT = 2;   // כמה ATR לשים לסטופ

    private static final int RISK_PER_TRADE = 20;
    private static final int COOLDOWN_FACTOR_CANT_TRADE = 3;
    private static final int CAN_TRADE = 0;
    private static int coolDownFromTrade = CAN_TRADE;

    public MWPatternBTCUSDT1H(ArrayList<Candle> candles) {
        super(candles);
        this.riskPerTradeUSD = RISK_PER_TRADE;
        this.riskReward = RR_RATIO;
    }

    @Override
    public void runBackTest() {
        coolDownFromTrade = CAN_TRADE;
        if (candles == null || candles.size() < START_PERIOD) {
            return;
        }

        for (int i = START_PERIOD; i < candles.size(); i++) {
            double atr = TrendUtils.calculateATR(candles, i, ATR_PERIOD);
            if (Double.isNaN(atr) || atr <= 0) {
                continue;
            }

            if (coolDownFromTrade > CAN_TRADE) {
                coolDownFromTrade--;
                continue;
            }

            Candle curr = candles.get(i);

            //  W-pattern -> LONG
            if (checkForW(i)) {
                Signal longSig  = createBuySignalATR(i, curr, atr, ATR_MULT, RR_RATIO);
                if (longSig != null) {
                    signals.add(longSig);
                    coolDownFromTrade = COOLDOWN_FACTOR_CANT_TRADE;
                }
            }
        }
    }

    private boolean checkForM(int currIndex) {
        return new MPatternTest(candles, currIndex).analyzeMPattern();
    }

    private boolean checkForW(int currIndex) {
        return new WPatternBTCUSDT1H(candles, currIndex).analyzeWPattern();
    }
}
