package com.lifecell.diia.be.model.dto.lifecell.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifecell.diia.be.model.fsm.State;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderStateInfo {
    @JsonProperty("ids")
    private List<String> ids;
    @JsonProperty("icon")
    private String icon;
    @JsonProperty("title")
    private String title;
    @JsonProperty("text")
    private String text;
    @JsonProperty("state")
    private State state;
    @JsonProperty("status")
    private String status;
    @JsonProperty("due")
    private Boolean due;
}
