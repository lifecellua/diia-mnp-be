package com.lifecell.diia.be.model.db.ext;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Jsonb {
    private String value;

    public static Jsonb of(String value) {
        return new Jsonb(value);
    }
}
