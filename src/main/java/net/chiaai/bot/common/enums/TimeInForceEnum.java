package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TimeInForceEnum {

    GTC("成交为止, 一直有效"),
    IOC("无法立即成交(吃单)的部分就撤销"),
    FOK("无法全部立即成交就撤销"),
    GTX("无法成为挂单方就撤销");

    private String desc;

}
