package com.accionmfb.omnix.checkpoint.exception;

import com.accionmfb.omnix.checkpoint.commons.ResponseCodes;
import com.accionmfb.omnix.checkpoint.payload.OmnixBaseResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class SecurityViolationException extends RuntimeException{

    private int statusCode;
    private final OmnixBaseResponse baseResponse;
    private SecurityViolationException(){
        this.baseResponse = new OmnixBaseResponse();
        this.baseResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
        this.baseResponse.setResponseMessage("No message");
        this.statusCode = 200;
    }

    private SecurityViolationException(String message){
        super(message);
        this.baseResponse = new OmnixBaseResponse();
        this.baseResponse.setResponseMessage(message);
        this.baseResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
        this.statusCode = 200;
    }

    public static SecurityViolationException newInstance(){
        return new SecurityViolationException();
    }

    public SecurityViolationException withCode(String code){
        this.baseResponse.setResponseCode(code);
        this.statusCode = 200;
        return this;
    }

    public SecurityViolationException withMessage(String message){
        this.baseResponse.setResponseMessage(message);
        this.statusCode = 200;
        return this;
    }

    public SecurityViolationException withStatusCode(int statusCode){
        this.statusCode = statusCode;
        return this;
    }

    public SecurityViolationException addError(String error){
        if(Objects.nonNull(this.getBaseResponse().getErrors())){
            this.baseResponse.getErrors().add(error);
        }else{
            this.getBaseResponse().setErrors(new ArrayList<>());
            this.getBaseResponse().getErrors().add(error);
        }
        return this;
    }

    public SecurityViolationException addErrors(List<String> errors){
        if(Objects.nonNull(this.getBaseResponse().getErrors())){
            this.baseResponse.getErrors().addAll(errors);
        }else{
            this.getBaseResponse().setErrors(new ArrayList<>());
            this.getBaseResponse().getErrors().addAll(errors);
        }
        return this;
    }
}
