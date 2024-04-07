package com.accionmfb.omnix.checkpoint.util;

import com.accionmfb.omnix.checkpoint.commons.StringValues;

import java.util.Arrays;
import java.util.Objects;

public class OmnixSecurityApplicationUtil {

    public static boolean isAllNull(Object ...objects){
        return Arrays.stream(objects).allMatch(Objects::isNull);
    }
    public static <T> T returnOrDefault(T value, T defaultValue){
        return Objects.isNull(value) ? defaultValue : value;
    }

    public static String cleanToken(String token){
        return token.startsWith(StringValues.AUTH_KEY_BEARER_PREFIX) ?
                token.replace(StringValues.AUTH_KEY_BEARER_PREFIX, StringValues.EMPTY_STRING).trim() :
                token.trim();

    }
}
