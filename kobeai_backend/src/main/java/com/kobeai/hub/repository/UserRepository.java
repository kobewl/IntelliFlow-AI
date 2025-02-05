package com.kobeai.hub.repository;

import com.kobeai.hub.model.User;
import com.kobeai.hub.model.User.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    // 根据用户名模糊查询
    Page<User> findByUsernameContaining(String username, Pageable pageable);

    // 根据角色查询
    Page<User> findByUserRole(UserRole role, Pageable pageable);

    // 根据用户名和角色组合查询
    Page<User> findByUsernameContainingAndUserRole(String username, UserRole role, Pageable pageable);

    // 统计在指定时间之后创建的用户数
    long countByCreatedAtAfter(LocalDateTime dateTime);

    // 统计指定角色的用户数
    long countByUserRole(UserRole role);
}