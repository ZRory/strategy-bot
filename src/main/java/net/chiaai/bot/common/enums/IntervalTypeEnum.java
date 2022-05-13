package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IntervalTypeEnum {

    ONE_MINUTE("1m", ""),
    THREE_MINUTE("3m", ""),
    FIVE_MINUTE("5m", ""),
    FIFTEEN_MINUTE("15m", ""),
    THIRTY_MINUTE("30m", ""),
    ONE_HOUR("1h", ""),
    TWO_HOUR("2h", ""),
    FOUR_HOUR("4h", ""),
    SIX_HOUR("6h", ""),
    EIGHT_HOUR("8h", ""),
    TWELVE_HOUR("12h", ""),
    ONE_DAY("1d", ""),
    THREE_DAY("3d", "");

    private String code;
    private String desc;

}
