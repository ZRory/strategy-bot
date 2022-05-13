package net.chiaai.bot.conf.feign;

import com.alibaba.fastjson.JSON;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.feign.request.BaseRequest;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Objects;

@Slf4j
public class CustomerPostFormEncoder implements Encoder {

    @Override
    public void encode(Object o, Type type, RequestTemplate requestTemplate) throws EncodeException {
        String urlParam = getUrlParam(o);
        requestTemplate.body(urlParam);
    }

    public static <T> String getUrlParam(T t) {
        //非binance的请求直接输出json
        if (!(t instanceof BaseRequest)) {
            return JSON.toJSONString(t);
        }
        StringBuilder urlParam = new StringBuilder();
        ObjectParamMetadata metadata = ObjectParamMetadata.getMetadata(t.getClass());
        for (PropertyDescriptor objectProperty : metadata.getObjectProperties()) {
            try {
                Field field = ObjectParamMetadata.getField(t.getClass(), objectProperty);
                //设置可以操作私有成员
                field.setAccessible(true);
                //获取成员值
                Object value = field.get(t);
                //成员值为 Null 时，则不处理
                if (Objects.nonNull(value)) {
                    urlParam.append(field.getName()).append("=").append(value).append("&");
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                log.error("NoSuchFieldException | IllegalAccessException", e);
            }
        }
        if (urlParam.length() > 0) {
            //去除最后一个&字符
            urlParam.deleteCharAt(urlParam.length() - 1);
        }
        return urlParam.toString();
    }

}
