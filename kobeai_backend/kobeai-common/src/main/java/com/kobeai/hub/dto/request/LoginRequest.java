package com.kobeai.hub.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}