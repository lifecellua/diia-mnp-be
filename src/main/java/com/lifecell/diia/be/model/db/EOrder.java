package com.lifecell.diia.be.model.db;

import com.lifecell.diia.be.model.db.ext.EEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Table("orders")
public class EOrder implements EEntity {
    @Id
    private UUID id;
    @Column("created_at")
    private OffsetDateTime createdAt;
    @Column("modified_at")
    private OffsetDateTime modifiedAt;
    @Column("user_ref")
    private String userRef;
    @Column("ref")
    private String ref;
    @Column("created_on")
    private OffsetDateTime createdOn;
    @Column("due_on")
    private OffsetDateTime dueOn;
    @Column("msisdn")
    private String msisdn;
    @Column("state_id")
    private Integer stateId;
    @Column("stage_ref")
    private String stageRef;
    @Column("cancelable")
    private Boolean cancelable;
    @Column("updatable")
    private Boolean updatable;
    @Column("error_code")
    private Integer errorCode;
    @Column("error_ref")
    private String errorRef;
    @Column("error_info")
    private String errorInfo;
    @Column("tariff_ref")
    private String tariffRef;
    @Column("tariff_name")
    private String tariffName;
}
