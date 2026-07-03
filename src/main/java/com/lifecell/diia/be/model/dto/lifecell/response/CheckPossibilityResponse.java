package com.lifecell.diia.be.model.dto.lifecell.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class CheckPossibilityResponse extends Response {
    private OperatorName operatorName;

    public enum OperatorName {
        LIFECELL,
        VODAFONE,
        KYIVSTAR,
        INTERTELECOM,
        THREE_MOB("3MOB"),
        PEOPLENET,
        LIFECELL_DUMMY;

        private final String code;

        OperatorName() {
            this.code = this.name();
        }

        OperatorName(String code) {
            this.code = code;
        }

        @JsonValue
        public String getCode() {
            return code;
        }

        @JsonCreator
        public static OperatorName fromCode(String code) {
            for (OperatorName op : OperatorName.values()) {
                if (op.code.equals(code)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown OperatorName: " + code);
        }
    }
}
