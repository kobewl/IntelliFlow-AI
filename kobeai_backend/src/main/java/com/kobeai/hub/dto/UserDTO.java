package com.kobeai.hub.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserDTO implements Serializable {
    private Long id;
    private String username;
    private String email;
    private String avatar;
    // 不包含敏感信息如密码
}