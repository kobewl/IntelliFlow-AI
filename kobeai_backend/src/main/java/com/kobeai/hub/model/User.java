package com.kobeai.hub.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
@Where(clause = "is_deleted = 0") // 自动过滤已删除的记录
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(columnDefinition = "VARCHAR(255) COMMENT '用户头像URL'")
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'NORMAL' COMMENT '用户角色：NORMAL-普通用户 VIP-VIP会员 SVIP-SVIP会员 ADMIN-管理员'")
    private UserRole userRole = UserRole.NORMAL; // 默认为普通用户

    @Column(columnDefinition = "DATETIME COMMENT '会员开始时间'")
    private LocalDateTime membershipStartTime;

    @Column(columnDefinition = "DATETIME COMMENT '会员结束时间'")
    private LocalDateTime membershipEndTime;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0 COMMENT '0-未删除 1-已删除'")
    private Integer isDeleted = 0; // 软删除标记：0-未删除 1-已删除

    // 用户角色枚举
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
}