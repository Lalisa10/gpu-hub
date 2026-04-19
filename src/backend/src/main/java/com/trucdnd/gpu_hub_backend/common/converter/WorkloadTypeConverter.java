package com.trucdnd.gpu_hub_backend.common.converter;

import com.trucdnd.gpu_hub_backend.common.constants.Workload;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WorkloadTypeConverter implements AttributeConverter<Workload.Type, String> {

    @Override
    public String convertToDatabaseColumn(Workload.Type type) {
        return type == null ? null : type.dbValue;
    }

    @Override
    public Workload.Type convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Workload.Type.fromDbValue(dbData);
    }
}
