package com.lifecell.diia.be.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.lifecell.diia.be.exception.GenericException;
import com.lifecell.diia.be.model.fsm.Response;
import com.lifecell.diia.be.model.fsm.ResponseForm;
import com.lifecell.diia.be.model.fsm.ResponseAwait;
import com.lifecell.diia.be.model.fsm.ResponseNotify;
import com.lifecell.diia.be.service.JsonService;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProtoUtils {
    private static final ObjectMapper objectMapper = (new ObjectMapper()).findAndRegisterModules();

    public static <T> T convert(JsonService jsonService, Response response, Class<T> clazz) {
        if (response instanceof ResponseForm form) {
            return jsonService.convert(form.getForm(), clazz);
        } else if (response instanceof ResponseAwait await) {
            return jsonService.convertToNextStep(await.getTarget(), clazz);
        } else if (response instanceof ResponseNotify notify) {
            return jsonService.convertToResponseCode(notify.getProblem().getCode(), clazz);
        } else {
            throw new GenericException("Not supported");
        }
    }

    public static <T> T structToClass(Struct data, Class<T> clazz) {
        try {
            var json = JsonFormat.printer().print(data);
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new GenericException("Can't convert struct to class", e);
        }
    }
}
