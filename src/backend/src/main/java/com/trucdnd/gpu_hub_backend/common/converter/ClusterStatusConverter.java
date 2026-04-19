package com.trucdnd.gpu_hub_backend.common.converter;

import com.trucdnd.gpu_hub_backend.common.constants.Cluster;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClusterStatusConverter implements AttributeConverter<Cluster.Status, String> {

    @Override
    public String convertToDatabaseColumn(Cluster.Status status) {
        return status == null ? null : status.dbValue;
    }

    @Override
    public Cluster.Status convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Cluster.Status.fromDbValue(dbData);
    }
}
