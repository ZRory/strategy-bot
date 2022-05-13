package net.chiaai.bot.feign.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.annotation.Order;

@Getter
@Setter
public class BaseRequest {


    private Long recvWindow = 6000L;

    /**
     * 下单时间戳
     */
    private Long timestamp = System.currentTimeMillis();

    /**
     * 签名值
     */
    @Order(value = Integer.MAX_VALUE)
    private String signature;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (recvWindow != null) {
            result.append("recvWindow=");
            result.append(recvWindow);
        }
        if (timestamp != null) {
            if (result.length() > 0) {
                result.append("&");
            }
            result.append("timestamp=").append(timestamp);
        }
        if (signature != null) {
            if (result.length() > 0) {
                result.append("&");
            }
            result.append("signature=").append(signature);
        }
        return result.toString();
    }
}
