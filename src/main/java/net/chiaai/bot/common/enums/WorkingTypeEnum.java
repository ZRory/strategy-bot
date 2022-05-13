package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkingTypeEnum {

    CONTRACT_PRICE("合约最新价"),
    MARK_PRICE("标记价格");

    private String desc;

}
