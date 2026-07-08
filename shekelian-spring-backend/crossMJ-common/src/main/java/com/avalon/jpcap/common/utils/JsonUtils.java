package com.avalon.jpcap.common.utils;

/**
 * @project: everyThingUtils
 * @description:
 * @author: DingHaoLun
 * @create: 2022-01-27 16:13
 **/

import com.avalon.jpcap.common.exceptions.UtilException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Objects;

public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    public static final ObjectMapper MAPPER;

    public JsonUtils() {
    }

    public static String toJson(Object content) {
        if (Objects.isNull(content)) {
            return null;
        } else {
            try {
                return MAPPER.setSerializationInclusion(Include.NON_NULL).writeValueAsString(content);
            } catch (JsonProcessingException var2) {
                log.error("Json serializer error", var2);
                throw new UtilException("json serializer error", var2);
            }
        }
    }

    public static <T> T fromJson(String jsonData, Class<T> clz) {
        Preconditions.checkNotNull(jsonData, "json data is null");

        try {
            return MAPPER.readValue(jsonData, clz);
        } catch (Exception var3) {
            log.error("Json string deserilizer error,jsonData：" + jsonData, var3);
            throw new UtilException("json deserilizer error", var3);
        }
    }

    public static <T> T fromJson(String jsonData, TypeReference<T> ref) {
        Preconditions.checkNotNull(jsonData, "json data is null");

        try {
            return MAPPER.readValue(jsonData, ref);
        } catch (JsonProcessingException var3) {
            log.error("Json string deserilizer error,jsonData：" + jsonData, var3);
            throw new UtilException("json deserilizer error", var3);
        }
    }

    public static <T> T fromJson(InputStream inputStream, Class<T> clz) {
        Preconditions.checkNotNull(inputStream, "json data is null");

        try {
            return MAPPER.readValue(inputStream, clz);
        } catch (Exception var3) {
            log.error("Json inputStream deserilizer error", var3);
            throw new UtilException("json inputStream deserilizer error", var3);
        }
    }

    public static <T> T fromJson(InputStream inputStream, TypeReference<T> ref) {
        Preconditions.checkNotNull(inputStream, "json data is null");

        try {
            return MAPPER.readValue(inputStream, ref);
        } catch (Exception var3) {
            log.error("Json inputStream deserilizer error", var3);
            throw new UtilException("json inputStream deserilizer error", var3);
        }
    }

    public static String toJsonPretty(Object data) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (JsonProcessingException var2) {
            throw new UtilException("invalid json object: " + data, var2);
        }
    }

    static {
        MAPPER = (new ObjectMapper()).configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }
}