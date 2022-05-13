package net.chiaai.bot.conf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class GlobalConfig {

    @Value("${global-config.enable-proxy}")
    private Boolean enableProxy;
    @Value("${global-config.proxy-host}")
    private String proxyHost;
    @Value("${global-config.proxy-port}")
    private String proxyPort;

    public static ExecutorService executor = new ThreadPoolExecutor(4, 8, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), new CustomizableThreadFactory("royal-mint-"));

    public static Key key = new SecretKeySpec("Qswl.188!!!!!!!!".getBytes(StandardCharsets.UTF_8), "AES");

    @PostConstruct
    private void setProxy() {
        if (enableProxy) {
            log.info("启用代理服务");
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", proxyPort);
        }
    }

}
