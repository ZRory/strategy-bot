package net.chiaai.bot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.chiaai.bot.entity.dao.BackTest;
import net.chiaai.bot.entity.dao.BackTestResult;
import org.springframework.scheduling.annotation.Async;

public interface BackTestService {
    BackTest createBackTest(BackTest backTest);

    @Async
    void execBackTest(BackTest backTest);

    Page<BackTest> list(Integer pageNum, Integer pageSize, String symbol, String status, String column, String sort);

    Page<BackTestResult> detail(Integer pageNum, Integer pageSize, Long backTestId, String column, String sort);

}
