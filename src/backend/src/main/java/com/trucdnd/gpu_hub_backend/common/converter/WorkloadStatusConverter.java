package com.trucdnd.gpu_hub_backend.common.converter;

import com.trucdnd.gpu_hub_backend.common.constants.Workload;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WorkloadStatusConverter implements AttributeConverter<Workload.Status, String> {

    @Override
    public String convertToDatabaseColumn(Workload.Status status) {
        return status == null ? null : status.dbValue;
    }

    @Override
    public Workload.Status convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Workload.Status.fromDbValue(dbData);
    }
}
