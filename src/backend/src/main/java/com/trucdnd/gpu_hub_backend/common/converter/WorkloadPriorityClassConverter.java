package com.trucdnd.gpu_hub_backend.common.converter;

import com.trucdnd.gpu_hub_backend.common.constants.Workload;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WorkloadPriorityClassConverter implements AttributeConverter<Workload.PriorityClass, String> {

    @Override
    public String convertToDatabaseColumn(Workload.PriorityClass priorityClass) {
        return priorityClass == null ? null : priorityClass.dbValue;
    }

    @Override
    public Workload.PriorityClass convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Workload.PriorityClass.fromDbValue(dbData);
    }
}
