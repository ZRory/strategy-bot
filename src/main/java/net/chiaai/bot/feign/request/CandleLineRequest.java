package net.chiaai.bot.feign.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.chiaai.bot.common.enums.IntervalTypeEnum;

@Getter
@Setter
@NoArgsConstructor
public class CandleLineRequest {

    private String symbol;

    /**
     * 默认查询一分钟数据
     */
    private String interval = IntervalTypeEnum.ONE_MINUTE.getCode();

    private Long startTime;

    private Long endTime;

    /**
     * 默认查720条数据
     */
    private Long limit = 720L;

    public CandleLineRequest(String symbol) {
        this.symbol = symbol;
    }
}
