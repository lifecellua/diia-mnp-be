package com.lifecell.diia.be.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lifecell.diia.be.model.dto.lifecell.response.OrderState;
import com.lifecell.diia.be.util.ProtoUtils;
import com.lifecell.diia.grpc.model.ApplicationStatusPostRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ApplicationStatus {

    public ApplicationStatus(ApplicationStatusPostRequest data) {
        this.applicationId = data.getApplicationId();
        this.status = OrderState.fromRef(data.getStatus());
        this.applicationData = ProtoUtils.structToClass(data.getApplicationData(), ApplicationData.class);
    }

    @NotBlank(message = "cannot be blank")
    private String applicationId;
    @NotNull(message = "cannot be null")
    private OrderState status;
    private ApplicationData applicationData;

    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplicationData {
        @NotNull(message = "cannot be null")
        @JsonProperty("cancelPossibility")
        private Boolean cancelPossibility;
        @NotBlank(message = "cannot be blank")
        @JsonProperty("tariffUa")
        private String tariffUa;
        @NotBlank(message = "cannot be blank")
        @JsonProperty("tariffCode")
        private String tariffCode;
        @JsonProperty("orderCreateDate")
        private OffsetDateTime orderCreateDate;
        @JsonProperty("dueDate")
        private OffsetDateTime dueDate;
    }
}
