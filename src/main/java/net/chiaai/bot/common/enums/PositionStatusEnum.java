package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 仓位状态
 */
@Getter
@AllArgsConstructor
public enum PositionStatusEnum {

    HOLD("持仓"),
    CLOSE("平仓");

    private String desc;

}
