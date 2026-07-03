package com.lifecell.diia.be.model.fsm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Session {
    private String userRef;
    private String userPhone;
    private String userName;
}
