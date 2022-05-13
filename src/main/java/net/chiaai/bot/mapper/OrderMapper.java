package net.chiaai.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.chiaai.bot.entity.dao.Order;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderMapper extends BaseMapper<Order> {
}
