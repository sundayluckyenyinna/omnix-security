package com.accionmfb.omnix.checkpoint.interceptors.data;

import com.accionmfb.omnix.checkpoint.instrumentation.BufferedRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterceptionData {
    private HttpServletRequest rawHttpServletRequest;
    private HttpServletResponse rawHttpServletResponse;
    private Object rawHandler;
}
