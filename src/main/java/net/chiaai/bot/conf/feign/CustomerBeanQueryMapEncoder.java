package net.chiaai.bot.conf.feign;

import feign.Param;
import feign.QueryMapEncoder;
import feign.codec.EncodeException;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class CustomerBeanQueryMapEncoder implements QueryMapEncoder {

    @Override
    public Map<String, Object> encode(Object object) throws EncodeException {
        try {
            ObjectParamMetadata metadata = ObjectParamMetadata.getMetadata(object.getClass());
            Map<String, Object> propertyNameToValue = new LinkedHashMap<>();
            for (PropertyDescriptor pd : metadata.getObjectProperties()) {
                Method method = pd.getReadMethod();
                Object value = method.invoke(object);
                if (value != null && value != object) {
                    Param alias = method.getAnnotation(Param.class);
                    String name = alias != null ? alias.value() : pd.getName();
                    propertyNameToValue.put(name, value);
                }
            }
            return propertyNameToValue;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new EncodeException("Failure encoding object into query map", e);
        }
    }

}
