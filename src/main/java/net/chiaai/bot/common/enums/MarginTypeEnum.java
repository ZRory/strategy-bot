package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MarginTypeEnum {

    ISOLATED("逐仓"),
    CROSSED("全仓");

    private String desc;

}
