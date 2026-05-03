package com.trucdnd.gpu_hub_backend.common.converter;

import com.trucdnd.gpu_hub_backend.common.constants.DataVolume;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataVolumeTypeConverter implements AttributeConverter<DataVolume.VolumeType, String> {

    @Override
    public String convertToDatabaseColumn(DataVolume.VolumeType type) {
        return type == null ? null : type.dbValue;
    }

    @Override
    public DataVolume.VolumeType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DataVolume.VolumeType.fromDbValue(dbData);
    }
}
