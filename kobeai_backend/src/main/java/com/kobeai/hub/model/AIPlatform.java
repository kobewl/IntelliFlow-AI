package com.kobeai.hub.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_platforms")
public class AIPlatform {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    @Enumerated(EnumType.STRING)
    private Platform type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String apiKey;

    @Column(nullable = false)
    private String baseUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 0代表系统AI，其他数字代表用户添加的AI
     */
    @Column(name = "user_id")
    private Long userId;
}