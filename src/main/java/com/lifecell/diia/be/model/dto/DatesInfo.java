package com.lifecell.diia.be.model.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DatesInfo {
    private String portingDate;
    private Boolean cancelingDateAvailable;
    private String cancelingDate;
}
