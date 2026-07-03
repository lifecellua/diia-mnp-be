package com.lifecell.diia.be.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifecell.diia.be.exception.GenericException;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@UtilityClass
public class ResourceUtils {
    private static final ObjectMapper objectMapper = (new ObjectMapper()).findAndRegisterModules()
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    public ObjectMapper getDefaultObjectMapper() {
        return objectMapper;
    }

    public String getJsonProperty(String json, String property) {
        try {
            return objectMapper.readTree(json).at(property).asText();
        } catch (IOException e) {
            throw new GenericException("Can't parse json and get property: {} -> {}", property, json, e);
        }
    }

    public <T> T getJsonProperty(String json, String property, TypeReference<T> clazz) {
        try {
            return objectMapper.treeToValue(objectMapper.readTree(json).at(property), clazz);
        } catch (IOException e) {
            throw new GenericException("Can't parse json and get property: {} -> {}", property, json, e);
        }
    }

    private static String loadJson(String name) {
        var fullName = name + ".json";
        try {
            var resource = new ClassPathResource(fullName);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GenericException("Can't load resource: {}", fullName, e);
        }
    }

    public static String loadDataJson(String name) {
        return loadJson("/data/" + name);
    }

    public static String loadFormJson(String name) {
        return loadJson("/forms/" + name);
    }

    public static String loadProblemJson(String name) {
        return loadJson("/problems/" + name);
    }

    public <T> T loadData(String name, TypeReference<T> clazz) {
        try {
            return objectMapper.readValue(loadDataJson(name), clazz);
        } catch (IOException e) {
            throw new GenericException("Can't parse data json: ", name, e);
        }
    }
}
