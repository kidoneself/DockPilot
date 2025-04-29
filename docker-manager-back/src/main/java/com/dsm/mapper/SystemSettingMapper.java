package com.dsm.mapper;

import com.dsm.pojo.entity.SystemSetting;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SystemSettingMapper {
    String getSettingValue(@Param("key") String key);

    void setSettingValue(@Param("key") String key, @Param("value") String value);

    List<SystemSetting> getAllSettings();
}