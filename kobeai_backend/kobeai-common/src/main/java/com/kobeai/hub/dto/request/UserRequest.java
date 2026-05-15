package com.kobeai.hub.dto.request;

import com.kobeai.hub.constant.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRequest {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String avatar;
    private UserRole userRole;
    private LocalDateTime membershipEndTime;
}