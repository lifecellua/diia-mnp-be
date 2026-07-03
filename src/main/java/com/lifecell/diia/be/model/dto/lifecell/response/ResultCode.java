package com.lifecell.diia.be.model.dto.lifecell.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ResultCode {
    SUCCESS(0, "Success validation"),
    BAD_INPUT(-1, "Bad input"),
    UNKNOWN_ERROR(-2, "Unknown error. System problem"),
    BAD_MSISDN(-5, "Bad msisdn"),
    BAD_ICCID(-6, "Bad iccid"),
    NOT_VALID_OTP(-7, "Not valid otp"),
    BAD_SIM_TYPE(-13, "Bad sim type"),
    NOT_FOUND_DOCUMENT(-42, "Not found document"),
    MSISDN_IS_LIFECELL(-47, "Msisdn is lifecell"),
    MSISDN_CANNOT_PORT(-48, "Msisdn can not portation because 30 days not ended after last portation"),
    INCORRECT_USER_FOR_THIS_ORDER(-49, "Incorrect user for this order"),
    INCORRECT_OR_NOT_VALID_TOKEN(-50, "Incorrect or not valid token"),
    MSISDN_IS_NOT_IN_THIS_ORDER(-51, "Msisdn is not in this order"),
    ORDER_HAS_A_FINAL_STATE(-52, "Order has a final state"),
    CAN_NOT_CANCEL_ORDER(-53, "Can not cancel order"),
    INCORRECT_INPUT_PARAMS(-54, "Incorrect input params"),
    CLIENT_FIND_IN_MSISDN_IN_OTHER_DOC(-55, "Client find in msisdn in other doc"),
    MSISDN_IS_BLOCKED(-56, "Msisdn is blocked"),
    MSISDN_EXISTS_IN_ACTIVE_ORDER(-57, "Msisdn exist in active order"),
    INCORRECT_TARIFF(-58, "Incorrect tariff"),
    MSISDN_NOT_UKRAINIAN(-59, "Msisdn is not Ukrainian"),
    INCORRECT_PUK_ONE(-61, "Incorrect puk1"),
    ICCID_BLOCKED(-62, "Iccid blocked"),
    ICCID_ALREADY_USED(-63, "Iccid already used"),
    INCORRECT_ORDER_STATE(-64, "Incorrect order state");

    private final int code;
    private final String description;

    ResultCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static ResultCode fromCode(int code) {
        for (ResultCode rc : ResultCode.values()) {
            if (rc.code == code) {
                return rc;
            }
        }
        return UNKNOWN_ERROR;
    }
}
