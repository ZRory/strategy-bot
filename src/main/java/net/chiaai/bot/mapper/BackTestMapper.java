package net.chiaai.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.chiaai.bot.entity.dao.BackTest;
import net.chiaai.bot.entity.dao.CandleLine;
import org.springframework.stereotype.Repository;

@Repository
public interface BackTestMapper extends BaseMapper<BackTest> {
}
