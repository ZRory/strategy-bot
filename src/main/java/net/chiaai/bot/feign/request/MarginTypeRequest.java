package net.chiaai.bot.feign.request;

import lombok.Data;
import net.chiaai.bot.common.enums.MarginTypeEnum;

@Data
public class MarginTypeRequest extends BaseRequest {

    private String symbol;

    private MarginTypeEnum marginType;

}
