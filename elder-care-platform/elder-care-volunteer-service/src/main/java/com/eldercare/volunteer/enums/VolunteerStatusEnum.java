package com.eldercare.volunteer.enums;

/**
 * 志愿者业务状态枚举。
 */
public enum VolunteerStatusEnum {
    PROFILE_NORMAL(2, "志愿者档案正常"),
    AVAILABLE_TIME(1, "可服务时间可用"),
    TIME_LOCKED(1, "时间已锁定"),
    TIME_RELEASED(2, "时间已释放");

    private final Integer code;
    private final String message;

    VolunteerStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer code() {
        return code;
    }

    public String message() {
        return message;
    }
}
