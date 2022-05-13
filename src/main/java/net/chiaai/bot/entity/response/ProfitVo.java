package net.chiaai.bot.entity.response;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProfitVo {

    private String symbol;

    /**
     * 最大占用金额
     */
    private BigDecimal maxAmount;

    /**
     * 当前总盈亏
     */
    private BigDecimal totalProfit;

    /**
     * 止盈金额
     */
    private BigDecimal targetProfit;

    /**
     * 止损金额
     */
    private BigDecimal stopProfit;

    /**
     * 手续费
     */
    private BigDecimal serviceAmount;

    /**
     * 已实现盈亏
     */
    private BigDecimal currentProfit;

    /**
     * 浮动盈亏
     */
    private BigDecimal slidingProfit;

    public ProfitVo(String symbol, List<PositionInfo> positionInfos) {
        this.symbol = symbol;
        if (CollectionUtils.isEmpty(positionInfos)) {
            return;
        }
        //计算总已实现盈亏
        this.currentProfit = positionInfos.stream().map(x -> x.getProfitAmount() == null ? BigDecimal.ZERO : x.getProfitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        //计算止盈金额
        this.targetProfit = positionInfos.stream().map(x -> x.getProfitAmount() == null || x.getProfitAmount().compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : x.getProfitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        //计算止损金额
        this.stopProfit = positionInfos.stream().map(x -> x.getProfitAmount() == null || x.getProfitAmount().compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ZERO : x.getProfitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        //计算浮动盈亏
        this.slidingProfit = positionInfos.stream().map(x -> x.getSlidingProfitAmount() == null ? BigDecimal.ZERO : x.getSlidingProfitAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        //计算服务费
        this.serviceAmount = positionInfos.stream().map(x -> x.getServiceAmount() == null ? BigDecimal.ZERO : x.getServiceAmount()).reduce(BigDecimal.ZERO, BigDecimal::subtract);
        //总盈亏=两者相加 - 服务费
        this.totalProfit = currentProfit.add(slidingProfit).add(serviceAmount);
        PositionInfo positionInfo = positionInfos.stream().filter(x -> x.getLevel() == 1).findAny().get();
        //计算最大仓位金额
        this.maxAmount = positionInfo.getPositionAmount().add(positionInfos.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(s -> s.getLevel()))), ArrayList::new)).stream().map(x -> x.getPositionAmount() == null ? BigDecimal.ZERO : x.getPositionAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

}
