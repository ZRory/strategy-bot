package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 策略状态
 */
@Getter
@AllArgsConstructor
public enum StrategyStatusEnum {

    //NEW("新建"),
    RUNNING("运行中"),
    PAUSE("已暂停"),
    ERROR("运行出错"),
    STOP("已停止");

    private String desc;

}
