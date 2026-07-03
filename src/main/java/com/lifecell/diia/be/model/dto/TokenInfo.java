package com.lifecell.diia.be.model.dto;

import com.lifecell.diia.be.model.dto.lifecell.response.ResultCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenInfo {
    private ResultCode resultCode;
    private String token;
}
