package com.accionmfb.omnix.checkpoint.payload;

import com.accionmfb.omnix.checkpoint.commons.CustomerValidationParam;
import lombok.Data;

@Data
public class CustomerValidationMatrix {
    private boolean isEnforceCustomerValidation;
    private CustomerValidationParam validationParam;
    private String queryField;
    private String statusField;
    private String[] statusAgainst;
}
