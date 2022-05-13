package net.chiaai.bot.service;

import net.chiaai.bot.common.enums.OrderSideEnum;
import net.chiaai.bot.common.enums.PositionSideEnum;
import net.chiaai.bot.entity.dao.Position;
import net.chiaai.bot.entity.dao.StrategyConfig;

import java.math.BigDecimal;

public interface DealService {

    void createPosition(StrategyConfig strategyConfig, OrderSideEnum orderSide, PositionSideEnum positionSide, Integer positionLevel, BigDecimal prePositionPrice);

    void closePosition(StrategyConfig strategyConfig, Position position, Boolean force);

}
