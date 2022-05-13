package net.chiaai.bot.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import net.chiaai.bot.entity.dao.CandleLine;
import org.springframework.scheduling.annotation.Async;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CandleLineService {

    CandleLine getCandleLine(String symbol, LocalDateTime openTime);

    public CandleLine getMin(String symbol, LocalDateTime startTime, LocalDateTime endTime, SFunction<CandleLine, ?> column);

    public CandleLine getMax(String symbol, LocalDateTime startTime, LocalDateTime endTime, SFunction<CandleLine, ?> column);

    @Async
    void update(String symbolName);

    List<String> listSymbols();
}
