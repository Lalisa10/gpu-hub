package com.trucdnd.gpu_hub_backend.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class DataSource {
    private DataSource() {}

    public enum Status {
        FORMATING("formating"),
        FORMATED("formated");

        @JsonValue
        public final String dbValue;
        Status(String dbValue) { this.dbValue = dbValue; }

        @JsonCreator
        public static Status fromDbValue(String v) {
            for (Status s : values()) if (s.dbValue.equals(v)) return s;
            throw new IllegalArgumentException("Unknown data source status: " + v);
        }
    }
}
