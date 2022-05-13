package net.chiaai.bot.conf.feign;


import feign.Feign;
import feign.Retryer;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {

    /**
     * @Description 替换解析queryMap的类为自定义map类
     */
    @Bean
    public Feign.Builder feignBuilder() {
        return Feign.builder()
                .queryMapEncoder(new CustomerBeanQueryMapEncoder())
                .retryer(Retryer.NEVER_RETRY);
    }

    @Bean
    public Encoder feignFormEncoder() {
        return new CustomerPostFormEncoder();
    }

    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return new CustomerErrorDecoder();
    }

}