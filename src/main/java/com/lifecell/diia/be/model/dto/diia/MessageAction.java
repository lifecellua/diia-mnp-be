package com.lifecell.diia.be.model.dto.diia;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageAction {
    @JsonProperty("type")
    private String type;
    @JsonProperty("subtype")
    private MessageActionSubtype subType;
    @JsonProperty("resourceId")
    private String resourceId;
}
