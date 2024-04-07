package com.accionmfb.omnix.checkpoint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUserDisabledEndpoint {
    private Long id;
    private Long appUserId;
    private Long connectionId;
    private Long serviceId;
    private String endpointUri;
    private LocalDateTime blackListedAt;
    private String blackListedBy;
}
