package com.eldercare.volunteer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.volunteer.entity.VolunteerCheckinRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 志愿者签到记录 Mapper。
 */
@Mapper
public interface VolunteerCheckinRecordMapper extends BaseMapper<VolunteerCheckinRecord> {
}
