package net.chiaai.bot.entity.dao;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName(value = "candle_line")
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class CandleLine {

    @JsonIgnore
    @TableField(value = "symbol")
    private String symbol;

    @TableField(value = "open_time")
    private Long openTime;

    @TableField(value = "open")
    private BigDecimal open;

    @TableField(value = "high")
    private BigDecimal high;

    @TableField(value = "low")
    private BigDecimal low;

    @TableField(value = "close")
    private BigDecimal close;

    @TableField(value = "volume")
    private BigDecimal volume;

    @TableField(value = "close_time")
    private Long closeTime;

    @TableField(exist = false)
    private BigDecimal quoteAssetVolume;

    @TableField(exist = false)
    private Long numberOfTrades;

    @TableField(exist = false)
    private BigDecimal takerBuyBaseAssetVolume;

    @TableField(exist = false)
    private BigDecimal takerBuyQuoteAssetVolume;

    @TableField(exist = false)
    private String meaningless;

}


