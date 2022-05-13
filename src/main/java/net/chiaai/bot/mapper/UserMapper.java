package net.chiaai.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.chiaai.bot.entity.dao.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends BaseMapper<User> {
}
