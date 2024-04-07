package com.accionmfb.omnix.checkpoint.registry;

import com.accionmfb.omnix.checkpoint.commons.RegistryItem;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OmnixSecurityRegistry {

    private static final ConcurrentMap<RegistryItem, Object> SECURITY_REGISTRY = new ConcurrentHashMap<>();

    public static void registerItem(RegistryItem item, Object value){
        if(Objects.nonNull(item) & Objects.nonNull(value)){
            SECURITY_REGISTRY.put(item, value);
        }
    }

    public static Object retrieveItemValue(RegistryItem item){
        if(Objects.nonNull(item)){
            return SECURITY_REGISTRY.get(item);
        }
        return null;
    }
}
