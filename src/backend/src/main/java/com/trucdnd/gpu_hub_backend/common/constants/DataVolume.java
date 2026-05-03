package com.trucdnd.gpu_hub_backend.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class DataVolume {
    private DataVolume() {}

    public enum VolumeType {
        DYNAMIC("dynamic"),
        SOURCE("source");

        @JsonValue
        public final String dbValue;
        VolumeType(String dbValue) { this.dbValue = dbValue; }

        @JsonCreator
        public static VolumeType fromDbValue(String v) {
            for (VolumeType t : values()) if (t.dbValue.equals(v)) return t;
            throw new IllegalArgumentException("Unknown volume type: " + v);
        }
    }
}
