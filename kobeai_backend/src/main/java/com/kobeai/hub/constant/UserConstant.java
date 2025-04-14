package com.kobeai.hub.constant;

/**
 * 用户相关常量
 *
 * @author renli
 */
public final class UserConstant {

    private UserConstant() {
        // 私有构造函数，防止实例化
    }

    /**
     * 角色相关常量
     */
    public static final class Role {
        public static final String USER_ADMIN = "ADMIN";
        public static final String USER_SVIP = "SVIP";
        public static final String USER_NORMAL = "NORMAL";
    }
}
