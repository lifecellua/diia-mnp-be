package com.lifecell.diia.be.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CustomerIdDocumentType {
    ID_CODE("taxpayer-card", "Ідентифікацфйний код"),
    PASSPORT_INT("internal-passport", "ID-паспорт"),
    PASSPORT_EXT("foreign-passport", "Закордонний паспорт"),
    DRIVER_LICENCE("driver-license", "Посвідчення водія"),
    PENSION_CARD("pension-card", "Пенсійне посвідчення");

    private final String ref;
    private final String title;

    private CustomerIdDocumentType(String ref, String title) {
        this.ref = ref;
        this.title = title;
    }

    @JsonValue
    public String getRef() {
        return ref;
    }

    public String getTitle() {
        return title;
    }

    @JsonCreator
    public static CustomerIdDocumentType fromRef(String ref) {
        for (CustomerIdDocumentType type : CustomerIdDocumentType.values()) {
            if (type.ref.equalsIgnoreCase(ref)) {
                return type;
            }
        }
        return null;
    }
}
