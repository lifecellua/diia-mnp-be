package com.lifecell.diia.be.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerIdDocument {
    @JsonProperty("documentType")
    private CustomerIdDocumentType type;
    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("secondName")
    private String secondName;
    @JsonProperty("middleName")
    private String middleName;
    @JsonProperty("birthday")
    private String birthday;
    @JsonProperty("documentSeries")
    private String series;
    @JsonProperty("documentNumber")
    private String number;
    @JsonProperty("documentAuthority")
    private String authority;
    @JsonProperty("documentDate")
    private String fromDate;
    @JsonProperty("documentExpDate")
    private String toDate;
    @JsonProperty("gender")
    private String gender;
    @JsonProperty("email")
    private String email;
    @JsonProperty("addressRegistration")
    private String address;
    @JsonProperty("taxpayerNumber")
    private String code;
}
