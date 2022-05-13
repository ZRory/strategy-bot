package net.chiaai.bot.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.BizCodeEnum;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.entity.dto.BollBand;
import net.chiaai.bot.entity.dto.Indicator;
import net.chiaai.bot.entity.dao.CandleLine;
import org.springframework.util.CollectionUtils;

import java.awt.geom.Line2D;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IndicatorService
 *
 * @author zhanghangtian
 * @description
 * @date 2021/12/9
 **/
@Slf4j
public class IndicatorUtil {

    /**
     * 计算TR指标
     */
    public static BigDecimal calcTR(CandleLine prevCandleLine, CandleLine currentCandleLine) {
        //TR : MAX(MAX((HIGH-LOW),ABS(REF(CLOSE,1)-HIGH)),ABS(REF(CLOSE,1)-LOW));
        return currentCandleLine.getHigh().subtract(currentCandleLine.getLow()).max(prevCandleLine.getClose().subtract(currentCandleLine.getHigh()).abs()).max(prevCandleLine.getClose().subtract(currentCandleLine.getLow()).abs());
    }

    /**
     * 计算EMA
     *
     * @param candleLines K线
     * @param times  要计算的时长，常用 5 10 15 30 60
     */
    public static Map<Long, BigDecimal> calEma(List<CandleLine> candleLines, Integer times) {
        Map emas = new HashMap<LocalDateTime, BigDecimal>();
        BigDecimal k = new BigDecimal(2.0 / (times + 1.0));// 计算出序数
        BigDecimal ema = BigDecimal.ZERO;
        for (int i = 0; i < candleLines.size(); i++) {
            CandleLine candleLine = candleLines.get(i);
            if (i == 0) {
                // 第一天ema等于当天收盘价
                ema = candleLine.getClose();
                emas.put(candleLine.getOpenTime(), ema);
            } else {
                // 第二天以后，当天收盘 收盘价乘以系数再加上昨天EMA乘以系数-1
                ema = (candleLine.getClose().multiply(k).add(ema.multiply((BigDecimal.ONE.subtract(k))))).setScale(8, RoundingMode.DOWN);
                emas.put(candleLine.getOpenTime(), ema);
            }
        }
        return emas;
    }

    /**
     * calculate MACD values
     *
     * @param candleLines      :kline
     * @param shortPeriod :the short period value.
     * @param longPeriod  :the long period value.
     * @return
     */
    public static List<Indicator> calMACD(List<CandleLine> candleLines, int shortPeriod, int longPeriod) {
        //1.先计算ema
        Map<Long, BigDecimal> shortEmas = calEma(candleLines, shortPeriod);
        Map<Long, BigDecimal> longEmas = calEma(candleLines, longPeriod);
        List<Indicator> indicators = new ArrayList<>();
        for (int i = 0; i < candleLines.size(); i++) {
            CandleLine candleLine = candleLines.get(i);
            Indicator indicator = new Indicator();
            indicator.setOpenTime(candleLine.getOpenTime());
            BigDecimal dif = shortEmas.get(candleLine.getOpenTime()).subtract(longEmas.get(candleLine.getOpenTime()));
            indicator.setDif(dif);
            if (i == 0) {
                // 第一天dea等于dif
                indicator.setDea(indicator.getDif());
            } else {
                // 第二天以后，(pre(dea)*8/10) + (dif*2/10)
                BigDecimal preDea = indicators.get(indicators.size() - 1).getDea().multiply(new BigDecimal("8")).divide(BigDecimal.TEN).setScale(8, RoundingMode.DOWN);
                BigDecimal nowDea = dif.multiply(new BigDecimal("2")).divide(BigDecimal.TEN).setScale(8, RoundingMode.DOWN);
                indicator.setDea(preDea.add(nowDea));
            }
            indicator.setMacd(dif.subtract(indicator.getDea()));
            indicators.add(indicator);
        }
        return indicators;
    }

    /**
     * 计算ATR
     * 获取移动平均值 = max（high-low,ref(close)-high,ref(close)-low）
     */
    public static BigDecimal calcAtr(List<CandleLine> candleLines) {
        if (candleLines.size() > 17) {
            //10条线切不包含当前线
            candleLines = candleLines.subList(candleLines.size() - 16, candleLines.size() - 1);
        }

        List<BigDecimal> atrs = new ArrayList<>();
        for (int i = 0; i < candleLines.size(); i++) {

            CandleLine currentCandleLine = candleLines.get(i);
            BigDecimal atr = null;
            if (i == 0) {
                atr = currentCandleLine.getHigh().subtract(currentCandleLine.getLow());
            } else {
                CandleLine lastCandleLine = candleLines.get(i - 1);
                //当前最大值-最小值
                BigDecimal v1 = currentCandleLine.getHigh().subtract(currentCandleLine.getLow());
                //当前最高价-前一个收盘价 的绝对值
                BigDecimal v2 = toPositive(lastCandleLine.getClose().subtract(currentCandleLine.getHigh()));
                //前一个收盘价-当前最低价 的绝对值
                BigDecimal v3 = toPositive(lastCandleLine.getClose().subtract(currentCandleLine.getLow()));
                //求出三个数的最大值为当前TR
                BigDecimal tr = v1.max(v2).max(v3);
                //ATR = (19 * PDN +TR)/20
                atr = new BigDecimal("12").multiply(atrs.get(i - 1)).add(tr).divide(new BigDecimal("12"), 8, RoundingMode.DOWN);
            }
            atrs.add(atr);
        }
        //返回当前atr
        return atrs.get(atrs.size() - 1);
    }

    /**
     * 计算MACD振幅
     */
    private static BigDecimal getMACDAvg(List<Indicator> indicators) {
        if (indicators.size() > 20) {
            //10条线切不包含当前线
            indicators = indicators.subList(indicators.size() - 21, indicators.size() - 1);
        }
        BigDecimal average = indicators.stream().map(x -> toPositive(x.getMacd())).reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(indicators.size()), 8, RoundingMode.DOWN);
        return average;
    }

    /**
     * 判断两条线是否有交点
     */
    private static boolean lineIntersect(Long frontX, BigDecimal frontLineY1, BigDecimal
            frontLineY2, Long rearX, BigDecimal rearLineY1, BigDecimal rearLineY2) {
        //log.info("当前EMA差值：{}", rearKlineEma5.subtract(rearKlineEma10));
        return Line2D.linesIntersect(frontX, frontLineY1.doubleValue(), rearX, rearLineY1.doubleValue(), frontX, frontLineY2.doubleValue(), rearX, rearLineY2.doubleValue());
    }

    private static BigDecimal toPositive(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return value.negate();
        }
        return value;
    }

    public static BollBand getCurrentBoll(List<CandleLine> candleLines) {
        List<BollBand> bollBands = getBoll(candleLines);
        if (CollectionUtils.isEmpty(bollBands)) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "策略指标查询失败，请确保该币种至少已开市21天");
        }
        return bollBands.get(bollBands.size() - 1);
    }

    public static List<BollBand> getBoll(List<CandleLine> candleLines) {
        return getBoll(candleLines, 21);
    }

    /**
     * 布林带BOLL（n， k） 一般n默认取20，k取2, mb为计算好的中轨线
     * 中轨线MB: n日移动平均线 MA(n)
     * 上轨线：MB + 2*MD
     * 下轨线：MB - 2*MD
     * MD：n日方差
     *
     * @param candleLines
     * @param n
     * @return
     */
    public static List<BollBand> getBoll(List<CandleLine> candleLines, Integer n) {
        List<BollBand> bollBands = new ArrayList<>();
        for (int i = 0, len = candleLines.size(); i < len; i++) {
            if (i < n - 1) {
                continue;
            }
            BollBand tempBoll = new BollBand();
            Long openTime = candleLines.get(i).getOpenTime();
            tempBoll.setOpenTime(openTime);
            BigDecimal sumMB = BigDecimal.ZERO;
            BigDecimal sumMD = BigDecimal.ZERO;
            for (int j = n - 1; j >= 0; j--) {
                BigDecimal thisClose = candleLines.get(i - j).getClose();
                sumMB = sumMB.add(thisClose);
            }
            BigDecimal middle = sumMB.divide(new BigDecimal(n.toString()), 8, RoundingMode.DOWN);
            tempBoll.setMiddle(middle);
            for (int j = n - 1; j >= 0; j--) {
                BigDecimal thisClose = candleLines.get(i - j).getClose();
                BigDecimal cma = thisClose.subtract(middle); // C-MB
                sumMD = sumMD.add(cma.multiply(cma));
            }
            BigDecimal md = sqrt(sumMD.divide(new BigDecimal(n.toString()), 8, RoundingMode.DOWN), 8);
            tempBoll.setMd(md);
            BigDecimal baseValue = new BigDecimal(BigInteger.valueOf(2)).multiply(md);
            tempBoll.setUpper(middle.add(baseValue));
            tempBoll.setLower(middle.subtract(baseValue));
            bollBands.add(tempBoll);
        }
        return bollBands;
    }

    private static BigDecimal sqrt(BigDecimal value, int scale) {
        BigDecimal num2 = BigDecimal.valueOf(2);
        int precision = 100;
        MathContext mc = new MathContext(precision, RoundingMode.HALF_UP);
        BigDecimal deviation = value;
        int cnt = 0;
        while (cnt < precision) {
            deviation = (deviation.add(value.divide(deviation, mc))).divide(num2, mc);
            cnt++;
        }
        deviation = deviation.setScale(scale, BigDecimal.ROUND_HALF_UP);
        return deviation;
    }

}
