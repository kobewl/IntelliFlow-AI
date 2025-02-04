package com.kobeai.hub.dto.request;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String username;
    private String email;
    private String avatar;
    // 可以根据需要添加其他需要更新的字段
}