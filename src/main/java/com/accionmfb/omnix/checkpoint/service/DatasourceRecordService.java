package com.accionmfb.omnix.checkpoint.service;

import com.accionmfb.omnix.checkpoint.model.AppUserRecord;
import com.accionmfb.omnix.checkpoint.model.ConnectingService;

import java.util.Map;

public interface DatasourceRecordService {
    AppUserRecord getAppUser(String username);

    ConnectingService getConnectingServiceByBaseUrl(String baseUrl);

    Map<String, Object> getCustomerRecord(String queryValue);
}
