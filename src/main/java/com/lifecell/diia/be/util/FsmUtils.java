package com.lifecell.diia.be.util;

import com.lifecell.diia.be.exception.FsmException;
import com.lifecell.diia.be.model.fsm.Problem;
import com.lifecell.diia.be.model.fsm.Request;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FsmUtils {

    public static <T extends Request> T toRequest(Request request, Class<T> clazz) {
        try {
            return clazz.cast(request);
        } catch (ClassCastException e) {
            throw new FsmException(Problem.FAIL, "Invalid request type", e);
        }
    }
}
