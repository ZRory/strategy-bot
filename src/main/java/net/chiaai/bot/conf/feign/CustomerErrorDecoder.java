package net.chiaai.bot.conf.feign;


import com.alibaba.fastjson.JSON;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.BizCodeEnum;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.feign.response.BaseResponse;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class CustomerErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 400 && response.status() < 500) {
            if (response.body() != null) {
                String result = response.body().toString();
                log.info("调用feign接口异常:{}|{}", response.status(), result);
                if (response.status() == 401) {
                    throw new BizException(BizCodeEnum.PERMISSION_DENY, "无权限:API配置有误");
                }
                if (StringUtils.isNotBlank(result)) {
                    BaseResponse baseResponse = JSON.parseObject(result, BaseResponse.class);
                    throw new BizException(BizCodeEnum.CALL_SERVICE_ERROR, result);
                }
                throw new BizException(BizCodeEnum.CALL_SERVICE_ERROR, "调用binance服务异常:" + result);
            }
        }
        // 如果不是500，还是交给Default去解码处理
        // 改进：这里使用单例即可，Default不用每次都去new
        return new ErrorDecoder.Default().decode(methodKey, response);
    }

}