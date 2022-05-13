package net.chiaai.bot.conf.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * @title: MybatisPlusConfig
 * @description: mybatis-plus分页组件配置类
 * @date 2021/9/29 11:19 @version1.0
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * mybatis-plus分页插件<br>
     */
    // 最新版
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean(name = "transactionManager")
    @Primary
    public DataSourceTransactionManager multipleTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}
