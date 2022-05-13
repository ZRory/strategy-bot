package net.chiaai.bot.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.BizCodeEnum;
import net.chiaai.bot.common.enums.FilterTypeEnum;
import net.chiaai.bot.common.enums.SymbolStatusEnum;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.conf.GlobalConfig;
import net.chiaai.bot.conf.env.BinanceCatchs;
import net.chiaai.bot.conf.feign.ObjectParamMetadata;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.request.BaseRequest;
import net.chiaai.bot.feign.request.CandleLineRequest;
import net.chiaai.bot.feign.response.Filter;
import net.chiaai.bot.entity.dao.CandleLine;
import net.chiaai.bot.feign.response.PriceResponse;
import net.chiaai.bot.feign.response.Symbol;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class BinanceUtil {

    @Resource
    private BinanceFeignClient binanceClient;

    public static <T extends BaseRequest> T encodeParams(T t, User user) {
        // URL 参数存储器
        StringBuilder urlParam = new StringBuilder();
        ObjectParamMetadata metadata = ObjectParamMetadata.getMetadata(t.getClass());
        for (PropertyDescriptor objectProperty : metadata.getObjectProperties()) {
            try {
                Field field = ObjectParamMetadata.getField(t.getClass(), objectProperty);
                //设置可以操作私有成员
                field.setAccessible(true);
                //获取成员值
                Object value = field.get(t);
                //成员值为 Null 时，则不处理
                if (Objects.nonNull(value)) {
                    urlParam.append(field.getName()).append("=").append(value).append("&");
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                log.error("NoSuchFieldException | IllegalAccessException", e);
            }
        }
        if (urlParam.length() > 0) {
            //去除最后一个&字符
            urlParam.deleteCharAt(urlParam.length() - 1);
        }
        //encode
        //解码secret
        String apiSecretEncode = user.getApiSecret();
        if (StringUtils.isBlank(apiSecretEncode)) {
            log.error("用户未配置api");
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "加签失败:用户未配置api");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // 确定算法
            cipher.init(Cipher.DECRYPT_MODE, GlobalConfig.key); // 进入解密模式
            String apiSecret = new String(cipher.doFinal(Base64.decodeBase64(apiSecretEncode)));// 解密
            String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, apiSecret).hmacHex(urlParam.toString());
            t.setSignature(signature);
            return t;
        } catch (Exception e) {
            log.error("secret解码失败", e);
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "secret解码失败");
        }
    }

    /**
     * 根据买入金额和现在金额计算买入数量
     */
    public BigDecimal calcQuantity(String symbolName, BigDecimal amount) {
        Symbol symbol = BinanceCatchs.symbols.get(symbolName);
        if (symbol == null || symbol.getStatus() == SymbolStatusEnum.CLOSE || symbol.getStatus() == SymbolStatusEnum.PENDING_TRADING) {
            log.error("交易对：{} 不存在或不处于可交易状态", symbolName);
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "交易对： " + symbolName + " 不存在或不处于可交易状态");
        }
        //查询币种最新价格
        PriceResponse priceResp = binanceClient.price(symbolName);
        //总金额/价格=数量
        BigDecimal quantity = amount.divide(priceResp.getPrice(), 10, RoundingMode.HALF_DOWN);
        //处理quantity精度问题
        for (Filter filter : symbol.getFilters()) {
            if (filter.getFilterType() == FilterTypeEnum.MARKET_LOT_SIZE) {
                int scale = filter.getStepSize().stripTrailingZeros().scale();
                quantity = quantity.setScale(scale, RoundingMode.HALF_DOWN);
                if (quantity.compareTo(filter.getMinQty()) < 0) {
                    throw new BizException(BizCodeEnum.OPERATION_FAILED, "金额过小，请增加仓位金额!单笔最小买入数量:" + filter.getMinQty() + ",最小买入金额(首仓金额*合约倍数):" + priceResp.getPrice().multiply(filter.getMinQty()));
                }
                if (quantity.compareTo(filter.getMaxQty()) > 0) {
                    throw new BizException(BizCodeEnum.OPERATION_FAILED, "金额过大，请减小仓位金额!单笔最大买入数量:" + filter.getMaxQty() + ",最大买入金额(首仓金额*合约倍数):" + priceResp.getPrice().multiply(filter.getMaxQty()));
                }
            }
        }
        return quantity;
    }

    public BigDecimal calcQuantity(String symbolName, BigDecimal amount, BigDecimal price) {
        Symbol symbol = BinanceCatchs.symbols.get(symbolName);
        if (symbol == null || symbol.getStatus() == SymbolStatusEnum.CLOSE || symbol.getStatus() == SymbolStatusEnum.PENDING_TRADING) {
            log.error("交易对：{} 不存在或不处于可交易状态", symbolName);
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "交易对： " + symbolName + " 不存在或不处于可交易状态");
        }
        //总金额/价格=数量
        BigDecimal quantity = amount.divide(price, 10, RoundingMode.HALF_DOWN);
        //处理quantity精度问题
        for (Filter filter : symbol.getFilters()) {
            if (filter.getFilterType() == FilterTypeEnum.MARKET_LOT_SIZE) {
                int scale = filter.getStepSize().stripTrailingZeros().scale();
                quantity = quantity.setScale(scale, RoundingMode.HALF_DOWN);
                if (quantity.compareTo(filter.getMinQty()) < 0) {
                    throw new BizException(BizCodeEnum.OPERATION_FAILED, "金额过小，请增加仓位金额!单笔最小买入数量:" + filter.getMinQty() + ",最小买入金额(首仓金额*合约倍数):" + price.multiply(filter.getMinQty()));
                }
                if (quantity.compareTo(filter.getMaxQty()) > 0) {
                    throw new BizException(BizCodeEnum.OPERATION_FAILED, "金额过大，请减小仓位金额!单笔最大买入数量:" + filter.getMaxQty() + ",最大买入金额(首仓金额*合约倍数):" + price.multiply(filter.getMaxQty()));
                }
            }
        }
        return quantity;
    }

    /**
     * 获取最近startTime的最高值（精度min） 最大12h
     *
     * @param symbol
     * @return
     */
    public BigDecimal getHighestPrice(String symbol, LocalDateTime startTime) {
        CandleLineRequest candleLineRequest = new CandleLineRequest(symbol);
        startTime = startTime.withSecond(0).withNano(0);
        if (startTime.plusHours(12).compareTo(LocalDateTime.now()) > 0) {
            candleLineRequest.setStartTime(startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }
        List<CandleLine> candleLines = binanceClient.candleLines(candleLineRequest);
        if (CollectionUtils.isEmpty(candleLines)) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "未查询到K线数据");
        }
        CandleLine maxCandleLine = candleLines.stream().max(Comparator.comparing(CandleLine::getHigh)).get();
        return maxCandleLine.getHigh();
    }

    /**
     * 获取最近startTime的最低值（精度min） 最大12h
     *
     * @param symbol
     * @return
     */
    public BigDecimal getLowestPrice(String symbol, LocalDateTime startTime) {
        CandleLineRequest candleLineRequest = new CandleLineRequest(symbol);
        startTime = startTime.withSecond(0).withNano(0);
        if (startTime.plusHours(12).compareTo(LocalDateTime.now()) > 0) {
            candleLineRequest.setStartTime(startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }
        List<CandleLine> candleLines = binanceClient.candleLines(candleLineRequest);
        if (CollectionUtils.isEmpty(candleLines)) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "未查询到K线数据");
        }
        CandleLine minCandleLine = candleLines.stream().min(Comparator.comparing(CandleLine::getLow)).get();
        return minCandleLine.getLow();
    }

}
