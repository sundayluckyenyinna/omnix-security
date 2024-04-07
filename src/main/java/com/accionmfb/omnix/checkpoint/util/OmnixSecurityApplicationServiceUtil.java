package com.accionmfb.omnix.checkpoint.util;

import com.accionmfb.omnix.checkpoint.annotation.EnforceCustomerValidation;
import com.accionmfb.omnix.checkpoint.annotation.RelaxCustomerValidation;
import com.accionmfb.omnix.checkpoint.commons.CustomerValidationParam;
import com.accionmfb.omnix.checkpoint.commons.StringValues;
import com.accionmfb.omnix.checkpoint.payload.CustomerValidationMatrix;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OmnixSecurityApplicationServiceUtil {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public PrintWriter getResponseWriter(HttpServletResponse servletResponse, HttpStatus httpStatus){
        servletResponse.setStatus(httpStatus.value());
        servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return servletResponse.getWriter();
    }

    @SneakyThrows
    public PrintWriter getResponseWriter(HttpServletResponse servletResponse, int statusCode){
        servletResponse.setStatus(statusCode);
        servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return servletResponse.getWriter();
    }

    public String resolveJwtToken(String rawToken){
        if(Objects.isNull(rawToken)){
            return null;
        }
        return rawToken.startsWith(StringValues.AUTH_KEY_BEARER_PREFIX) ?
                rawToken.replace(StringValues.AUTH_KEY_BEARER_PREFIX, StringValues.EMPTY_STRING).trim()
                : rawToken.trim();
    }

    public CustomerValidationMatrix getDecisionForCustomerValidationEnforcement(Method method, Class<?> declaringClass){
        CustomerValidationMatrix matrix = new CustomerValidationMatrix();
        matrix.setEnforceCustomerValidation(true);
        matrix.setValidationParam(CustomerValidationParam.EXISTENCE_ONLY);

        EnforceCustomerValidation mEcv = method.getAnnotation(EnforceCustomerValidation.class);
        EnforceCustomerValidation cEcv = declaringClass.getAnnotation(EnforceCustomerValidation.class);

        RelaxCustomerValidation mRcv = method.getAnnotation(RelaxCustomerValidation.class);
        RelaxCustomerValidation cRcv = declaringClass.getAnnotation(RelaxCustomerValidation.class);

        // Get the decision based on the presence on both the class and the method.
        boolean isFinalEnforceCustomerValidation;
        EnforceCustomerValidation finalCustomerValidationEnforcementAnnotation;
        if(Objects.nonNull(cEcv))    // There is the annotation to enforce customer validation at class level.
        {
            if(Objects.nonNull(mEcv)){
                isFinalEnforceCustomerValidation = true;
               finalCustomerValidationEnforcementAnnotation = mEcv;
               updateMatrixWithEnforcementAnnotationAndToEnforceValidation(matrix, finalCustomerValidationEnforcementAnnotation);
            }else{
                if(Objects.nonNull(mRcv)){
                    isFinalEnforceCustomerValidation = false;
                }
                else{
                    isFinalEnforceCustomerValidation = true;
                    finalCustomerValidationEnforcementAnnotation = cEcv;
                    updateMatrixWithEnforcementAnnotationAndToEnforceValidation(matrix, finalCustomerValidationEnforcementAnnotation);
                }
            }
        }
        else
        {
            if(Objects.nonNull(cRcv))  // There is the annotation to relax customer validation at class level.
            {
                if(Objects.nonNull(mEcv)){
                    isFinalEnforceCustomerValidation = true;
                    finalCustomerValidationEnforcementAnnotation = mEcv;
                    updateMatrixWithEnforcementAnnotationAndToEnforceValidation(matrix, finalCustomerValidationEnforcementAnnotation);
                }
                else{
                    isFinalEnforceCustomerValidation = false;
                }
            }
            else{
                if(Objects.nonNull(mEcv)){
                    isFinalEnforceCustomerValidation = true;
                    finalCustomerValidationEnforcementAnnotation = mEcv;
                    updateMatrixWithEnforcementAnnotationAndToEnforceValidation(matrix, finalCustomerValidationEnforcementAnnotation);
                }else{
                    isFinalEnforceCustomerValidation = true;
                    finalCustomerValidationEnforcementAnnotation = DefaultCustomerValidationAnnotation.class.getAnnotation(EnforceCustomerValidation.class);
                    updateMatrixWithEnforcementAnnotationAndToEnforceValidation(matrix, finalCustomerValidationEnforcementAnnotation);
                }
            }
        }

        try{ log.info("Resolved Validation Matrix: {}", objectMapper.writeValueAsString(matrix)); }
        catch (Exception ignored){}
        matrix.setEnforceCustomerValidation(isFinalEnforceCustomerValidation);
        return matrix;
    }

    private void updateMatrixWithEnforcementAnnotationAndToEnforceValidation(CustomerValidationMatrix matrix, EnforceCustomerValidation ann){
        matrix.setQueryField(ann.queryField());
        matrix.setStatusAgainst(ann.statusAgainst());
        matrix.setStatusField(ann.statusField());
        matrix.setValidationParam(ann.validateFor());
    }

    @EnforceCustomerValidation
    public static class DefaultCustomerValidationAnnotation{}
}
