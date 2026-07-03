package com.lifecell.diia.be.model.db;

import com.lifecell.diia.be.model.db.ext.EEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Table("flows_history")
public class EFlowHistory implements EEntity {
    @Id
    private UUID id;
    @Column("flow_id")
    private AggregateReference<EFlow, UUID> flow;
    @Column("modified_at")
    private OffsetDateTime modifiedAt;
    @Column("user_ref")
    private String userRef;
    @Column("state_id")
    private Integer stateId;
    @Column("stage_ref")
    private String stageRef;
    @Column("error_code")
    private Integer errorCode;
    @Column("error_ref")
    private String errorRef;
    @Column("tariff_ref")
    private String tariffRef;
    @Column("tariff_name")
    private String tariffName;
    @Column("msisdn")
    private String msisdn;
    @Column("token")
    private String token;
    @Column("iccid")
    private String iccid;
    @Column("document_id")
    private AggregateReference<EDocument, UUID> document;
    @Column("order_id")
    private AggregateReference<EOrder, UUID> order;
}
