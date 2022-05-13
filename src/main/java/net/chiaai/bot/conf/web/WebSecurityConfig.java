package net.chiaai.bot.conf.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    @Bean
    public RedisSessionInterceptor getSessionInterceptor() {
        return new RedisSessionInterceptor();
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        //serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
        return serializer;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getSessionInterceptor()).addPathPatterns("/**")
                //                .excludePathPatterns("/swagger-ui.html")
                //                .excludePathPatterns("/configuration/ui")
                //                .excludePathPatterns("/swagger-resources/**")
                //                .excludePathPatterns("/configuration/security")
                //                .excludePathPatterns("/v2/api-docs")
                //                .excludePathPatterns("/webjars/**")
                .excludePathPatterns("/error")
                //                .excludePathPatterns("/**/favicon.ico")
                .excludePathPatterns("/**/user/register")
                .excludePathPatterns("/**/user/getGraphCode")
                .excludePathPatterns("/**/user/getSmsCode")
                .excludePathPatterns("/**/user/findPassword")
                .excludePathPatterns("/**/user/login")
                .excludePathPatterns("/**/user/logout")
                .excludePathPatterns("/**/bot/start")
                .excludePathPatterns("/**/bot/stop");
    }
}
