package com.lifecell.diia.be.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifecell.diia.be.exception.GenericException;
import com.lifecell.diia.be.util.ResourceUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class JsonService {
    private static final String M_NEW_BUILDER = "newBuilder";
    private static final String M_BUILD ="build";
    private static final String M_PREFIX_SET = "set";
    private static final String M_PREFIX_ADD = "add";
    private static final String F_PROCESS_CODE = "processCode";
    private static final String F_TEMPLATE = "template";
    private static final String F_NEXT_STEP = "nextStep";

    private final ObjectMapper om;

    private static <T> Iterable<T> toIterable(Iterator<T> iterator) {
        return () -> iterator;
    }

    private static <T> Stream<T> toStream(Iterator<T> iterator) {
        var spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    private static String formatMessage(String template, Object... args) {
        return MessageFormatter.arrayFormat(template, args).getMessage();
    }

    public JsonService() {
        this.om = ResourceUtils.getDefaultObjectMapper();
    }

    public <T> T convertToResponseCode(Integer responseCode, Class<T> clazz) {
        var path = "";
        var builder = invokeNewBuilder(path, clazz);

        {
            var setter = locateSetter(path, builder.getClass(), M_PREFIX_SET, F_PROCESS_CODE);
            invokeSetter(path, builder, setter, Math.abs(responseCode));
        }

        var obj = invokeBuild(path, builder);
        @SuppressWarnings("unchecked")
        T t = (T) obj;
        return t;
    }

    public <T> T convertToNextStep(String text, Class<T> clazz) {
        var path = "";
        var builder = invokeNewBuilder(path, clazz);

        {
            var setter = locateSetter(path, builder.getClass(), M_PREFIX_SET, F_NEXT_STEP);
            invokeSetter(path, builder, setter, text);
        }

        var obj = invokeBuild(path, builder);
        @SuppressWarnings("unchecked")
        T t = (T) obj;
        return t;
    }

    public <T> T convert(String text, Class<T> clazz) {
        var root = readTree(text);
        if (!root.isObject()) {
            throw new GenericException(formatMessage("Gson (): root must be an object"));
        }
        @SuppressWarnings("unchecked")
        T t = (T) convertObject("", root, clazz);
        return t;
    }

    private JsonNode readTree(String text) {
        try {
            return om.readTree(text);
        } catch (Exception e) {
            throw new GenericException(formatMessage("Gson (): can't parse' json"), e);
        }
    }

    private Object convertObject(String path, JsonNode node, Class<?> clazz) {
        var builder = invokeNewBuilder(path, clazz);
        for (var field: toIterable(node.fields())) {
            var trail = path.isBlank() ? field.getKey() : path + "." + field.getKey();
            if (field.getValue().isArray()) {
                var setter = locateManySetter(path, builder.getClass(), field.getKey());
                setManyValue(trail, builder, setter, field.getValue());
            } else {
                var setter = locateOneSetter(path, builder.getClass(), field.getKey());
                setOneValue(trail, builder, setter, field.getValue());
            }
        }
        var obj = invokeBuild(path, builder);
        return obj;
    }

    private Object invokeNewBuilder(String path, Class<?> clazz) {
        try {
            var m = clazz.getDeclaredMethod(M_NEW_BUILDER);
            var v = m.invoke(null);
            return v;
        } catch (NoSuchMethodException e) {
            throw new GenericException(formatMessage("Gson ({}): class {} has no 'newBuilder' method", path, clazz.getSimpleName()) , e);
        } catch (Exception e) {
            throw new GenericException(formatMessage("Gson ({}): class {} can't invoke 'newBuilder' method", path, clazz.getSimpleName()) , e);
        }
    }

    private Object invokeBuild(String path, Object obj) {
        try {
            var m = obj.getClass().getDeclaredMethod(M_BUILD);
            var v = m.invoke(obj);
            return v;
        } catch (NoSuchMethodException e) {
            throw new GenericException(formatMessage("Gson ({}): class {} has no 'build' method", path, obj.getClass().getSimpleName()), e);
        } catch (Exception e) {
            throw new GenericException(formatMessage("Gson ({}): class {} can't invoke 'build' method", path, obj.getClass().getSimpleName()) , e);
        }
    }

    private Method locateSetter(String path, Class<?> clazz, String prefix, String name) {
        var fn = name.substring(0,1).toUpperCase() + name.substring(1);
        var pfn = prefix + fn;
        for (var method : clazz.getDeclaredMethods()) {
            if (pfn.equals(method.getName())
                    && (method.getReturnType() == clazz)
                    && (method.getParameterCount() == 1)
                    && (method.getParameterTypes()[0] != clazz)
                    && (!(method.getParameterTypes()[0].getSimpleName().equalsIgnoreCase("Builder")))) {
                return method;
            }
        }
        throw new GenericException(formatMessage("Gson '{}': can't locate method '{}'",path, name));
    }

    private Method locateOneSetter(String path, Class<?> clazz, String name) {
        return locateSetter(path, clazz, M_PREFIX_SET, name);
    }

    private Method locateManySetter(String path, Class<?> clazz, String name) {
        return locateSetter(path, clazz, M_PREFIX_ADD, name);
    }

    private Class<?> guessSetterParameter(Method setter) {
        return setter.getParameterTypes()[0];
    }

    private void invokeSetter(String path, Object obj, Method method, Object value) {
        try {
            method.invoke(obj, value);
        } catch (Exception e) {
            throw new GenericException(formatMessage("Gson '{}': cant invoke setter '{}' for value '{}'", path, method.getName(), value.toString()), e);
        }
    }

    private void setOneValue(String path, Object builder, Method setter, JsonNode value) {
        var clazz = guessSetterParameter(setter);
        if (value.isNull()) {
            //skip
        } else if (value.isBoolean() && clazz.isAssignableFrom(boolean.class)) {
            invokeSetter(path, builder, setter, value.asBoolean());
        }else if (value.isNumber() && clazz.isAssignableFrom(double.class)) {
                invokeSetter(path, builder, setter, value.asDouble());
        } else if (value.isNumber() && clazz.isAssignableFrom(float.class)) {
            invokeSetter(path, builder, setter, (float) value.asDouble());
        } else if (value.isNumber() && clazz.isAssignableFrom(long.class)) {
            invokeSetter(path, builder, setter, value.asLong());
        } else if (value.isNumber() && clazz.isAssignableFrom(int.class)) {
            invokeSetter(path, builder, setter, value.asInt());
        } else if (value.isTextual() && clazz.isEnum()) {
            var v = value.asText();
            var found = false;
            for (var vv : clazz.getEnumConstants()) {
                if (vv.toString().equalsIgnoreCase(v)) {
                    invokeSetter(path, builder, setter, vv);
                    found = true;
                    break;
                }
            }
            if (!found) {
                var names = Stream.of(clazz.getEnumConstants()).map(Object::toString).collect(Collectors.joining(","));
                throw new GenericException(formatMessage("Gson '{}': can't set value '{}' from one of '{}'", path, v, names));
            }
        } else if (value.isTextual() && clazz.isAssignableFrom(String.class)) {
            var v = value.asText();
            invokeSetter(path, builder, setter, v);
        } else if (value.isObject()) {
            var v = convertObject(path, value, clazz);
            invokeSetter(path, builder, setter, v);
        } else {
            var v = value.asText();
            throw new GenericException(formatMessage("Gson '{}': can't set value '{}' as '{}'", path, v, clazz.getSimpleName()));
        }
    }

    private void setManyValue(String path, Object builder, Method setter, JsonNode value) {
        var clazz = guessSetterParameter(setter);
        var idx = 0;
        for (var element: toIterable(value.elements())) {
            var trail = path + "["  + idx + "]";
            setOneValue(trail, builder, setter, element);
            idx = idx + 1;
        }
    }
}
