package net.chiaai.bot.feign.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountInfoResponse extends BaseResponse {

    //手续费等级
    private Integer feeTier;
    // 是否可以交易
    private Boolean canTrade;
    // 是否可以入金
    private Boolean canDeposit;
    // 是否可以出金
    private Boolean canWithdraw;

    //当前所需起始保证金总额(存在逐仓请忽略), 仅计算usdt资产
    private BigDecimal totalInitialMargin;
    //维持保证金总额, 仅计算usdt资产
    private BigDecimal totalMaintMargin;
    //账户总余额, 仅计算usdt资产
    private BigDecimal totalWalletBalance;
    //持仓未实现盈亏总额, 仅计算usdt资产
    private BigDecimal totalUnrealizedProfit;
    //保证金总余额, 仅计算usdt资产
    private BigDecimal totalMarginBalance;
    //持仓所需起始保证金(基于最新标记价格), 仅计算usdt资产
    private BigDecimal totalPositionInitialMargin;
    //当前挂单所需起始保证金(基于最新标记价格), 仅计算usdt资产
    private BigDecimal totalOpenOrderInitialMargin;
    //全仓账户余额, 仅计算usdt资产
    private BigDecimal totalCrossWalletBalance;
    //全仓持仓未实现盈亏总额, 仅计算usdt资产
    private BigDecimal totalCrossUnPnl;
    //可用余额, 仅计算usdt资产
    private BigDecimal availableBalance;
    //最大可转出余额, 仅计算usdt资产
    private BigDecimal maxWithdrawAmount;

}
