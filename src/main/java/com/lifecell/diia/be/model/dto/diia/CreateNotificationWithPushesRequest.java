package com.lifecell.diia.be.model.dto.diia;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateNotificationWithPushesRequest {
    @JsonProperty("userIdentifier")
    private String userIdentifier;
    @JsonProperty("itn")
    private String itn;
    @JsonProperty("templateCode")
    private String templateCode;
    @JsonProperty("resourceId")
    private String resourceId;
    @JsonProperty("excludeMobileUids")
    private List<String> excludeMobileUids;
    @JsonProperty("appVersion")
    private NotificationCampaignAppVersion appVersion;
    @JsonProperty("action")
    private MessageAction action;
}
