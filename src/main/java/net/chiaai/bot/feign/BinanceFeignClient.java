package net.chiaai.bot.feign;

import net.chiaai.bot.entity.dao.CandleLine;
import net.chiaai.bot.feign.request.*;
import net.chiaai.bot.feign.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "binance-service", url = "${binance.url}")
public interface BinanceFeignClient {


    /**
     * 创建订单
     */
    @PostMapping(value = "/fapi/v1/order", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    OrderResponse createOrder(@RequestHeader(value = "X-MBX-APIKEY") String apikey, @RequestBody OrderRequest request);

    /**
     * 调整杠杆倍率
     */
    @PostMapping(value = "/fapi/v1/leverage", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    LeverageResponse leverage(@RequestHeader(value = "X-MBX-APIKEY") String apikey, @RequestBody LeverageRequest request);

    /**
     * 变换逐全仓模式
     */
    @PostMapping(value = "/fapi/v1/marginType", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    BaseResponse marginType(@RequestHeader(value = "X-MBX-APIKEY") String apikey, @RequestBody LeverageRequest request);

    /**
     * 变更持仓模式
     */
    @PostMapping(value = "/fapi/v1/positionSide/dual", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    BaseResponse updatePositionSide(@RequestHeader(value = "X-MBX-APIKEY") String apikey, @RequestBody PositionSideRequest request);

    /**
     * 查询持仓模式
     */
    @GetMapping(value = "/fapi/v1/positionSide/dual")
    PositionSideResponse getPositionSide(@RequestHeader(value = "X-MBX-APIKEY") String apikey, @SpringQueryMap BaseRequest request);

    /**
     * 查询交易limit等信息
     */
    @GetMapping(value = "/fapi/v1/exchangeInfo")
    ExchangeInfoResp exchangeInfo();

    /**
     * 查询价格
     */
    @GetMapping(value = "/fapi/v1/ticker/price")
    PriceResponse price(@RequestParam String symbol);


    /**
     * 查询k线
     */
    @GetMapping(value = "/fapi/v1/klines")
    List<CandleLine> candleLines(@SpringQueryMap CandleLineRequest request);

    /**
     * 查询手续费率
     */
    @GetMapping(value = "/fapi/v1/commissionRate")
    ServiceAmountRateResponse serviceAmountRate(@RequestHeader(value = "X-MBX-APIKEY") String apikey, @SpringQueryMap ServiceAmountRateRequest request);

    /**
     * 查询账户余额
     */
    @GetMapping(value = "/fapi/v2/balance")
    List<BalanceResponse> balance(@RequestHeader(value = "X-MBX-APIKEY") String apikey, @SpringQueryMap BaseRequest request);

    /**
     * 查询账户信息
     */
    @GetMapping(value = "/fapi/v2/account")
    AccountInfoResponse account(@RequestHeader(value = "X-MBX-APIKEY") String apikey, @SpringQueryMap BaseRequest request);

}
