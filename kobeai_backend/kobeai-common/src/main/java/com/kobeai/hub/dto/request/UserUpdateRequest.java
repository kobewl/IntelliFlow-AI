package com.kobeai.hub.dto.request;

import com.kobeai.hub.constant.UserRole;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserUpdateRequest {
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private UserRole userRole;
    private LocalDateTime membershipEndTime;
    // 可以根据需要添加其他需要更新的字段
}