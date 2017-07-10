package com.jfrog.xray.client.impl.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class that provides the ObjectMapper for all Details classes
 *
 * @author Dan Feldman
 */
public class ObjectMapperHelper {

    public static ObjectMapper get() {
        return buildDetailsMapper();
    }

    private static ObjectMapper buildDetailsMapper() {
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
