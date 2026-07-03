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
@Table("documents")
public class EDocument implements EEntity {
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
    @Column("state_id")
    private Integer stateId;
    @Column("error_code")
    private Integer errorCode;
    @Column("error_ref")
    private String errorRef;
    @Column("error_info")
    private String errorInfo;
}
