package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SymbolStatusEnum {

    PENDING_TRADING("待上市"),
    TRADING("交易中"),
    PRE_DELIVERING("预交割"),
    DELIVERING("交割中"),
    DELIVERED("已交割"),
    PRE_SETTLE("预结算"),
    SETTLING("结算中"),
    CLOSE("已下架");

    private String desc;

}
