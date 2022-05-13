package net.chiaai.bot.common.enums;

import java.io.Serializable;

public interface BizEnum extends Serializable {

    Integer getCode();

    String getName();

    String getDesc();

}
