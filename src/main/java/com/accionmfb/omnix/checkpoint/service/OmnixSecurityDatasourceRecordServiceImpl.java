package com.accionmfb.omnix.checkpoint.service;

import com.accionmfb.omnix.checkpoint.commons.Role;
import com.accionmfb.omnix.checkpoint.config.properties.SecurityProperties;
import com.accionmfb.omnix.checkpoint.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OmnixSecurityDatasourceRecordServiceImpl implements DatasourceRecordService{

    private final SecurityProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;


    @Override
    public AppUserRecord getAppUser(String username){
        String sql = String.format("select * from %s where username = ?", properties.getAppUserLookupTableName());
        AppUserRecord appUserRecord = getRecordFromQueryMap(jdbcTemplate.queryForList(sql, username).stream().findFirst().orElse(new HashMap<>()), AppUserRecord.class);
        if(Objects.isNull(appUserRecord)){
            return null;
        }

        AppUserConnection appUserConnection = getAppUserConnection(appUserRecord.getConnectionId());
        if(Objects.nonNull(appUserConnection)){
            List<AppUserConnectingIP> connectingIPS = getAppUserConnectionIps(appUserRecord.getId(), appUserConnection.getId());
            List<AppUserDisabledEndpoint> disabledEndpoints = getAppUserDisabledEndpoints(appUserRecord.getId(), appUserConnection.getId());
            List<AppUserConnectingService> connectingServices = getAppUserConnectingService(appUserRecord.getId(), appUserConnection.getId());
            addConnectingIPsToAppUserConnection(connectingIPS, appUserConnection);
            addBlackListedEndpointsToAppUserConnection(disabledEndpoints, appUserConnection);
            addConnectingServicesToAppUserConnection(connectingServices, appUserConnection);
            appUserRecord.setConnection(appUserConnection);
        }
        else{
            appUserRecord.setConnectionId(-1L);
        }

        AppUserRole appUserRole = getAppUserRole(appUserRecord.getRoleId());
        appUserRole = Objects.isNull(appUserRole) ? getDefaultUnknownRole() : appUserRole;
        appUserRecord.setAppUserRole(appUserRole);
        return appUserRecord;
    }

    @Override
    public ConnectingService getConnectingServiceByBaseUrl(String baseUrl){
        String sql = "select * from connecting_service where base_url = ?";
        Map<String, Object> map = jdbcTemplate.queryForList(sql, baseUrl).stream().findFirst().orElse(new HashMap<>());
        return getRecordFromQueryMap(map, ConnectingService.class);
    }

    @Override
    public Map<String, Object> getCustomerRecord(@NonNull String queryValue){
        String sql = String.format("select * from %s where %s = ?", properties.getCustomerLookupTableName(), properties.getCustomerQueryFieldName());
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, queryValue);
        return records.stream().findFirst().orElse(new HashMap<>());
    }

    private void addConnectingIPsToAppUserConnection(List<AppUserConnectingIP> connectingIPS, AppUserConnection appUserConnection){
        if(connectingIPS.isEmpty()){
            appUserConnection.setConnectingIps(new ArrayList<>());
        }
        else{
            appUserConnection.setConnectingIps(connectingIPS.stream().map(AppUserConnectingIP::getIpAddress).collect(Collectors.toList()));
        }
    }

    private void addBlackListedEndpointsToAppUserConnection(List<AppUserDisabledEndpoint> disabledEndpoints, AppUserConnection appUserConnection){
        if(disabledEndpoints.isEmpty()){
            appUserConnection.setDisabledEndpoints(new ArrayList<>());
        }
        else{
            appUserConnection.setDisabledEndpoints(disabledEndpoints.stream().map(AppUserDisabledEndpoint::getEndpointUri).collect(Collectors.toList()));
        }
    }

    private void addConnectingServicesToAppUserConnection(List<AppUserConnectingService> connectingServices, AppUserConnection appUserConnection){
        if(connectingServices.isEmpty()){
            appUserConnection.setConnectingServices(new ArrayList<>());
        }else{
            appUserConnection.setConnectingServices(connectingServices);
        }
    }

    public List<AppUserConnectingIP> getAppUserConnectionIps(Long appUserId, Long connectionId){
        String sql = "select * from app_user_connecting_ip where app_user_id = ? and connection_id = ?";
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql, appUserId, connectionId);
        return getRecordFromQueryList(maps, AppUserConnectingIP.class);
    }

    public AppUserConnection getAppUserConnection(Long connectionId){
        String sql = "select * from app_user_connection where id = ?";
        Map<String, Object> queryMap = jdbcTemplate.queryForList(sql, connectionId).stream().findFirst().orElse(new HashMap<>());
        return getRecordFromQueryMap(queryMap, AppUserConnection.class);
    }

    public List<AppUserDisabledEndpoint> getAppUserDisabledEndpoints(Long appUserId, Long connectionId){
        String sql = "select * from app_user_disabled_endpoint where app_user_id = ? and connection_id = ?";
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql, appUserId, connectionId);
        return getRecordFromQueryList(maps, AppUserDisabledEndpoint.class);
    }

    public List<AppUserConnectingService> getAppUserConnectingService(Long appUserId, Long connectionId){
        String sql = "select * from app_user_connecting_service where app_user_id = ? and connection_id = ?";
        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql, appUserId, connectionId);
        return getRecordFromQueryList(maps, AppUserConnectingService.class);
    }

    public ConnectingService getConnectingServiceById(Long id){
        String sql = "select * from connecting_service where id = ?";
        Map<String, Object> map = jdbcTemplate.queryForList(sql, id).stream().findFirst().orElse(new HashMap<>());
        return getRecordFromQueryMap(map, ConnectingService.class);
    }

    public AppUserRole getAppUserRole(Long roleId){
        String sql = "select * from app_user_role where id = ?";
        return getRecordFromQueryMap(jdbcTemplate.queryForList(sql, roleId).stream().findFirst().orElse(new HashMap<>()), AppUserRole.class);
    }

    private AppUserRole getDefaultUnknownRole(){
        return AppUserRole.builder()
                .id(System.currentTimeMillis())
                .roleName(Role.UNKNOWN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy("SYSTEM")
                .updatedBy("SYSTEM")
                .build();
    }

    @SneakyThrows
    private <T> T getRecordFromQueryMap(Map<String, Object> map, Class<T> tClass){
        if(Objects.isNull(map)){
            return null;
        }
        String mapJson = objectMapper.writeValueAsString(map);
        return objectMapper.readValue(mapJson, tClass);
    }

    @SneakyThrows
    private <T> List<T> getRecordFromQueryList(List<Map<String, Object>> maps, Class<T> tClass){
        if(maps.isEmpty()){
            return new ArrayList<>();
        }
        String mapJson = objectMapper.writeValueAsString(maps);
        return objectMapper.readValue(mapJson, objectMapper.getTypeFactory().constructCollectionType(List.class, tClass));
    }
}
