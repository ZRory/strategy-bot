package net.chiaai.bot.conf.feign;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class ObjectParamMetadata {

    private static final Map<Class<?>, ObjectParamMetadata> CLASS_TO_METADATA =
            new HashMap<>();

    public static ObjectParamMetadata getMetadata(Class<?> objectType) {
        ObjectParamMetadata metadata = CLASS_TO_METADATA.get(objectType);
        if (metadata == null) {
            try {
                metadata = ObjectParamMetadata.parseObjectType(objectType);
            } catch (IntrospectionException e) {
                log.error("IntrospectionException", e);
            }
            CLASS_TO_METADATA.put(objectType, metadata);
        }
        return metadata;
    }

    private final List<PropertyDescriptor> objectProperties;

    private ObjectParamMetadata(List<PropertyDescriptor> objectProperties) {
        this.objectProperties = Collections.unmodifiableList(objectProperties);
    }

    private static ObjectParamMetadata parseObjectType(Class<?> type)
            throws IntrospectionException {
        List<PropertyDescriptor> properties = new ArrayList<>();

        for (PropertyDescriptor pd : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            boolean isGetterMethod = pd.getReadMethod() != null && !"class".equals(pd.getName());
            if (isGetterMethod) {
                properties.add(pd);
            }
        }

        /**
         * 实现排序
         */
        properties = properties.stream().sorted(Comparator.comparing(x -> {
            int orderValue = -1;
            try {
                Field field = getField(type, x);
                Order orderAnno = field.getAnnotation(Order.class);
                if (orderAnno != null) {
                    orderValue = orderAnno.value();
                }
            } catch (NoSuchFieldException e) {
                log.error("NoSuchFieldException", e);
            }
            return orderValue;
        })).collect(Collectors.toList());

        return new ObjectParamMetadata(properties);
    }

    public static Field getField(Class clazz, PropertyDescriptor objectProperty) throws NoSuchFieldException {
        Field field = null;
        while (field == null) {
            try {
                field = clazz.getDeclaredField(objectProperty.getName());
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
                if (clazz == null) {
                    throw e;
                }
            }
        }
        return field;
    }

}
