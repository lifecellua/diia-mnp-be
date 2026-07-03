package com.lifecell.diia.be.util;

import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import com.lifecell.diia.be.model.fsm.Problem;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;

@Slf4j
@UtilityClass
public class LifecellUtils {
    public static final DateTimeFormatter PRETTY_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    public static final DateTimeFormatter PRETTY_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter PRETTY_TIME = DateTimeFormatter.ofPattern("HH:mm");

    public static Problem toProblem(ResultCode rc) {
        switch (rc) {
            case SUCCESS: {
                return Problem.SUCCESS;
                //break;
            }
            //INVALIDS
            case BAD_MSISDN: {
                return Problem.INVALID_PHONE;
                //break;
            }
            case BAD_ICCID: {
                return Problem.INVALID_ICCID;
                //break;
            }
            case NOT_VALID_OTP: {
                return Problem.INVALID_OTP;
                //break;
            }
            case BAD_SIM_TYPE: {
                return Problem.INVALID_SIM;
                //break;
            }
            case MSISDN_IS_LIFECELL: {
                return Problem.INVALID_PHONE_OUR;
                //break;
            }
            case MSISDN_CANNOT_PORT: {
                return Problem.INVALID_PHONE_EARLY;
                //break;
            }
            case INCORRECT_PUK_ONE: {
                return Problem.INVALID_PUK1;
                //return;
            }
            case ICCID_BLOCKED: {
                return Problem.INVALID_ICCID_BLOCKED;
                //break;
            }
            case ICCID_ALREADY_USED: {
                return Problem.INVALID_ICCID_USED;
                //break;
            }
            //FAILS
            case INCORRECT_USER_FOR_THIS_ORDER: {
                return Problem.FAIL_INVALID_ORDER;
                //break;
            }
            case INCORRECT_OR_NOT_VALID_TOKEN: {
                return Problem.FAIL_INVALID_ORDER;
                //break;
            }
            case MSISDN_IS_NOT_IN_THIS_ORDER: {
                return Problem.FAIL_INVALID_ORDER;
                //break;
            }
            case CAN_NOT_CANCEL_ORDER: {
                return  Problem.FAIL_CAN_NOT_CANCEL_ORDER;
                //break;
            }
            case INCORRECT_INPUT_PARAMS: {
                return Problem.FAIL_INVALID_PHONE_TRACKED;
                //break;
            }
            case CLIENT_FIND_IN_MSISDN_IN_OTHER_DOC: {
                return Problem.FAIL_INVALID_PHONE_TRACKED;
                //break;
            }
            case MSISDN_IS_BLOCKED: {
                return Problem.FAIL_INVALID_PHONE_BLOCKED;
                //break;
            }
            case MSISDN_EXISTS_IN_ACTIVE_ORDER: {
                return Problem.FAIL_INVALID_PHONE_DUPLICATED;
                //break;
            }
            case INCORRECT_TARIFF: {
                return Problem.FAIL_INVALID_ORDER;
                //break;
            }
            case INCORRECT_ORDER_STATE: {
                return Problem.FAIL_INVALID_ORDER;
                //break;
            }
            default: {
                return Problem.FAIL;
            }
        }
    }
}
