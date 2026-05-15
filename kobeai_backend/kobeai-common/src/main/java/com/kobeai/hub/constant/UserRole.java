package com.kobeai.hub.constant;

/**
 * 用户角色枚举
 */
public enum UserRole {
    NORMAL("普通用户"),
    VIP("VIP会员"),
    SVIP("SVIP会员"),
    ADMIN("管理员");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
