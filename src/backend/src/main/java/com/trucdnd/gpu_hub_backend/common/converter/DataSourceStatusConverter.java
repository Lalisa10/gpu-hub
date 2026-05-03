package com.trucdnd.gpu_hub_backend.common.converter;

import com.trucdnd.gpu_hub_backend.common.constants.DataSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataSourceStatusConverter implements AttributeConverter<DataSource.Status, String> {

    @Override
    public String convertToDatabaseColumn(DataSource.Status status) {
        return status == null ? null : status.dbValue;
    }

    @Override
    public DataSource.Status convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DataSource.Status.fromDbValue(dbData);
    }
}
