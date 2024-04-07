package com.accionmfb.omnix.checkpoint.model;

import com.accionmfb.omnix.checkpoint.commons.AppUserStatus;
import com.accionmfb.omnix.checkpoint.commons.StringValues;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AppUserRecord {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private AppUserStatus status;
    private String username;
    private String password;
    private String channel;
    private String encryptionKey;
    private Long roleId;
    private Long connectionId;

    @JsonIgnore
    private AppUserConnection connection;

    @JsonIgnore
    private AppUserRole appUserRole;

    public boolean isEndpointBlackListed(String endpoint){
        if(Objects.nonNull(getConnection())){
            return getConnection().getDisabledEndpoints().contains(endpoint);
        }
        log.warn("No Connection was found associated to this particular app user with username: {}", getUsername());
        return true;
    }

    public boolean isHasServiceEnabled(ConnectingService connectingService){
        if(Objects.nonNull(getConnection())){
            return getConnection().getConnectingServices().stream().anyMatch(cs -> Objects.equals(cs.getServiceId(), connectingService.getId()));
        }
        return false;
    }

    public boolean isConnectingIPAllowed(String connectingIp){
        if(Objects.nonNull(getConnection())){
            List<String> connectingIps = getConnection().getConnectingIps();
            return connectingIps.contains(connectingIp) || connectingIps.contains(StringValues.STAR);
        }
        return false;
    }
}
