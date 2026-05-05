package com.eldercare.common.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户角色枚举。
 */
public enum RoleEnum {
    /** 老人本人。 */
    ELDER("ELDER", "老人"),
    /** 亲情号用户。 */
    FAMILY("FAMILY", "亲情号"),
    /** 志愿者或服务人员。 */
    VOLUNTEER("VOLUNTEER", "志愿者"),
    /** 后台管理员。 */
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String message;

    RoleEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public static Set<String> codes() {
        return Arrays.stream(values()).map(RoleEnum::code).collect(Collectors.toUnmodifiableSet());
    }
}
