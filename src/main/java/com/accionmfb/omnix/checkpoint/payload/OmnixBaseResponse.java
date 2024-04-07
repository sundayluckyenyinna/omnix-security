package com.accionmfb.omnix.checkpoint.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OmnixBaseResponse {
    private String responseCode;
    private String responseMessage;
    private List<String> errors = new ArrayList<>();
}
