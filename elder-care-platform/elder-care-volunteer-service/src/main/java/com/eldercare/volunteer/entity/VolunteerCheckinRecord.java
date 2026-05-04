package com.eldercare.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 志愿者签到记录实体。
 */
@TableName("volunteer_checkin_record")
public class VolunteerCheckinRecord {
    /** 主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 社区 ID。 */
    private Long communityId;
    /** 志愿者用户 ID。 */
    private Long volunteerUserId;
    /** 签到日期。 */
    private LocalDate checkinDate;
    /** 签到时间。 */
    private LocalDateTime checkinTime;
    /** 经度。 */
    private BigDecimal longitude;
    /** 纬度。 */
    private BigDecimal latitude;
    /** 签到地址。 */
    private String address;
    /** 签到状态：1 正常。 */
    private Integer status;

    /** 创建今日签到记录。 */
    public static VolunteerCheckinRecord today(Long communityId, Long volunteerUserId, BigDecimal longitude, BigDecimal latitude, String address) {
        // 构造签到记录。
        VolunteerCheckinRecord record = new VolunteerCheckinRecord();
        // 写入社区 ID。
        record.communityId = communityId;
        // 写入志愿者用户 ID。
        record.volunteerUserId = volunteerUserId;
        // 写入签到日期。
        record.checkinDate = LocalDate.now();
        // 写入签到时间。
        record.checkinTime = LocalDateTime.now();
        // 写入经度。
        record.longitude = longitude;
        // 写入纬度。
        record.latitude = latitude;
        // 写入地址。
        record.address = address;
        // 第一版签到默认正常。
        record.status = 1;
        // 返回记录。
        return record;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }
    public Long getVolunteerUserId() { return volunteerUserId; }
    public void setVolunteerUserId(Long volunteerUserId) { this.volunteerUserId = volunteerUserId; }
    public LocalDate getCheckinDate() { return checkinDate; }
    public void setCheckinDate(LocalDate checkinDate) { this.checkinDate = checkinDate; }
    public LocalDateTime getCheckinTime() { return checkinTime; }
    public void setCheckinTime(LocalDateTime checkinTime) { this.checkinTime = checkinTime; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
