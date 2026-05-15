package com.kobeai.hub.constant;

public class RedisKeyConstant {
    // User related keys
    public static final String USER_INFO_KEY = "user:info:"; // 用户信息缓存key前缀
    public static final String USER_TOKEN_KEY = "user:token:"; // 用户token缓存key前缀

    // Message related keys
    public static final String CHAT_MESSAGES_KEY = "chat:messages:"; // 聊天记录缓存key前缀
    public static final String USER_CONVERSATIONS_KEY = "user:conversations:"; // 用户会话列表缓存key前缀

    // System related keys
    public static final String SYSTEM_ANNOUNCEMENT_KEY = "system:announcement"; // 系统公告缓存key

    // TTL constants (in seconds)
    public static final long USER_INFO_TTL = 3600; // 用户信息缓存时间1小时
    public static final long TOKEN_TTL = 86400; // token缓存时间24小时
    public static final long MESSAGES_TTL = 7200; // 消息缓存时间2小时
    public static final long ANNOUNCEMENT_TTL = 86400; // 公告缓存时间24小时
}