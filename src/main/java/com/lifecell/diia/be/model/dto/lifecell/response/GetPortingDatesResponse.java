package com.lifecell.diia.be.model.dto.lifecell.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class GetPortingDatesResponse extends Response {
    private OffsetDateTime nearestPortingDate;
    private OffsetDateTime cancelProhibitionDate;
    private Boolean cancelProhibition = true;
}
