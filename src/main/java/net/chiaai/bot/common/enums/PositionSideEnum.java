package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 持仓方向，单向持仓模式下非必填，默认且仅可填BOTH;在双向持仓模式下必填,且仅可选择 LONG 或 SHORT
 */
@Getter
@AllArgsConstructor
public enum PositionSideEnum {

    BOTH("多空双向"),
    LONG("开多"),
    SHORT("开空");

    private String desc;

}
