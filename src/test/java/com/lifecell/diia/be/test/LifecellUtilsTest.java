package com.lifecell.diia.be.test;

import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.util.LifecellUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LifecellUtilsTest {

    @Test
    public void testPrettyDate() {
        var text = LocalDate.of(2020, 11, 21).format(LifecellUtils.PRETTY_DATE);
        assertEquals("21.11.2020", text);
    }

    @Test
    public void testPrettyDateTime() {
        var text = LocalDateTime.of(2020, 11, 21, 03, 45, 57).format(LifecellUtils.PRETTY_DATE_TIME);
        assertEquals("21.11.2020 03:45", text);
    }

    @Test
    public void testToProblemSUCCESS() {
        var problem = LifecellUtils.toProblem(ResultCode.SUCCESS);
        assertEquals(Problem.SUCCESS, problem);
    }

    @Test
    public void testToProblemINVALID_PHONE() {
        var problem = LifecellUtils.toProblem(ResultCode.BAD_MSISDN);
        assertEquals(Problem.INVALID_PHONE, problem);
    }

    @Test
    public void testToProblemINVALID_ICCID() {
        var problem = LifecellUtils.toProblem(ResultCode.BAD_ICCID);
        assertEquals(Problem.INVALID_ICCID, problem);
    }

    @Test
    public void testToProblemNOT_VALID_OTP() {
        var problem = LifecellUtils.toProblem(ResultCode.NOT_VALID_OTP);
        assertEquals(Problem.INVALID_OTP, problem);
    }

    @Test
    public void testToProblemINVALID_SIM() {
        var problem = LifecellUtils.toProblem(ResultCode.BAD_SIM_TYPE);
        assertEquals(Problem.INVALID_SIM, problem);
    }

    @Test
    public void testToProblemINVALID_PHONE_OUR() {
        var problem = LifecellUtils.toProblem(ResultCode.MSISDN_IS_LIFECELL);
        assertEquals(Problem.INVALID_PHONE_OUR, problem);
    }

    @Test
    public void testToProblemINVALID_PHONE_EARLY() {
        var problem = LifecellUtils.toProblem(ResultCode.MSISDN_CANNOT_PORT);
        assertEquals(Problem.INVALID_PHONE_EARLY, problem);
    }

    @Test
    public void testToProblemINVALID_PUK1() {
        var problem = LifecellUtils.toProblem(ResultCode.ICCID_BLOCKED);
        assertEquals(Problem.INVALID_ICCID_BLOCKED, problem);
    }

    @Test
    public void testToProblemINVALID_CAN_NOT_CANCEL_ORDER() {
        var problem = LifecellUtils.toProblem(ResultCode.CAN_NOT_CANCEL_ORDER);
        assertEquals(Problem.FAIL_CAN_NOT_CANCEL_ORDER, problem);
    }
}
