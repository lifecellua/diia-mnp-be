package com.lifecell.diia.be.model.dto.lifecell.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.lifecell.diia.be.util.ResourceUtils;

import java.util.List;

public enum OrderState {
    ORDER_ERROR("orderError"),
    IN_PROCESS("inProcess"),
    IVR_PROCESSING("IvrProcessing"),
    IVR_SUCCESS("IvrSuccess"),
    CDB_REJECT("cdbReject"),
    SENT_TO_DONOR("sentToDonor"),
    DONOR_REJECT_FINAL("donorRejectFinal"),
    DONOR_ACCEPT("donorAccept"),
    NP_CONTRACT("NPContract"),
    ACTIVATED_MSISDN("activatedMsisdn"),
    BROADCAST("Broadcast"),
    AUTO_CANCEL("autoCancel"),
    SUBSCRIBER_CANCEL("subscriberCancel"),
    CANCEL_BY_USER("cancelByUser"),
    CANCEL("cancel"),
    DONOR_ACCEPT_WAIT_SIM_CARD("donorAcceptWaitSimCard"),
    IN_PROGRESS_WAIT_SIM_CARD("inProcessWaitSimCard"),
    UNKNOWN("unknown");

    private static OrderStateInfo loadInfo(String ref) {
        var infos = ResourceUtils.loadData("order-states",
                new TypeReference<List<OrderStateInfo>>(){});
        for (var info : infos) {
            for (var id : info.getIds()) {
                if (id.equalsIgnoreCase(ref)) {
                    return info;
                }
            }
        }
        return loadInfo("unknown");
    }

    private final String ref;
    private final OrderStateInfo info;

    OrderState(String ref) {
        this.ref = ref;
        this.info = loadInfo(ref);
    }

    @JsonValue
    public String getRef() {
        return ref;
    }

    public OrderStateInfo getInfo() {
        return info;
    }

    @JsonCreator
    public static OrderState fromRef(String code) {
        for (OrderState status : OrderState.values()) {
            if (status.ref.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
