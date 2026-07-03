package com.lifecell.diia.be.util;

import com.lifecell.diia.be.exception.GenericException;
import lombok.experimental.UtilityClass;
import ua.gov.diia.grpc.SessionUtils;
import ua.gov.diia.grpc.interceptor.server.SessionServerInterceptor;
import ua.gov.diia.types.token.UserTokenDataMsg;

@UtilityClass
public class DiiaUtils {

    public static UserTokenDataMsg getSessionUser() {
        var session = SessionServerInterceptor.SESSION_KEY.get();
        var rawUtd = SessionUtils.getUserTokenData(session);
        if (rawUtd instanceof UserTokenDataMsg) {
            return (UserTokenDataMsg) rawUtd;
        }
        throw new GenericException("Can't get user token data from session");
    }
}
