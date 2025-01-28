package com.kobeai.hub.service.impl;

import com.kobeai.hub.dto.request.RegisterRequest;
import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.model.User;
import com.kobeai.hub.model.User.UserRole;
import com.kobeai.hub.repository.UserRepository;
import com.kobeai.hub.service.UserService;
import com.kobeai.hub.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public ApiResponse<?> login(String username, String password) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ApiResponse.error("密码错误");
            }

            String token = jwtUtil.generateToken(user);
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);

            return ApiResponse.success("登录成功", result);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public User getUserProfile(String token) {
        String username = jwtUtil.getUsernameFromToken(token);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public void logout(String token) {
        jwtUtil.invalidateToken(token);
    }

    @Override
    @Transactional
    public ApiResponse<?> register(RegisterRequest request) {
        try {
            // 检查用户名是否已存在
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ApiResponse.error("用户名已存在");
            }

            // 检查邮箱是否已存在
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ApiResponse.error("邮箱已被注册");
            }

            // 检查手机号是否已存在
            if (userRepository.findByPhone(request.getPhone()).isPresent()) {
                return ApiResponse.error("手机号已被注册");
            }

            // 创建新用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setUserRole(UserRole.NORMAL); // 设置默认角色

            // 保存用户
            User savedUser = userRepository.save(user);
            log.info("新用户注册成功: {}", savedUser.getUsername());

            // 生成token并返回
            String token = jwtUtil.generateToken(savedUser);
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", savedUser);

            return ApiResponse.success("注册成功", result);
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return ApiResponse.error("注册失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> updateUserRole(Long userId, UserRole newRole) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            user.setUserRole(newRole);
            userRepository.save(user);

            return ApiResponse.success("用户角色更新成功");
        } catch (Exception e) {
            log.error("更新用户角色失败", e);
            return ApiResponse.error("更新失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> updateMembership(Long userId, UserRole membershipType, int monthsDuration) {
        try {
            if (membershipType != UserRole.VIP && membershipType != UserRole.SVIP) {
                return ApiResponse.error("无效的会员类型");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime;

            // 如果用户已经是会员且未过期，则在现有结束时间基础上增加时长
            if (isActiveMember(userId)) {
                endTime = user.getMembershipEndTime().plusMonths(monthsDuration);
            } else {
                // 新会员从当前时间开始计算
                user.setMembershipStartTime(now);
                endTime = now.plusMonths(monthsDuration);
            }

            user.setUserRole(membershipType);
            user.setMembershipEndTime(endTime);
            userRepository.save(user);

            Map<String, Object> result = new HashMap<>();
            result.put("membershipType", membershipType);
            result.put("startTime", user.getMembershipStartTime());
            result.put("endTime", user.getMembershipEndTime());
            return ApiResponse.success("会员更新成功", result);
        } catch (Exception e) {
            log.error("更新会员信息失败", e);
            return ApiResponse.error("更新失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isActiveMember(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 检查是否是VIP或SVIP，且会员未过期
            return (user.getUserRole() == UserRole.VIP || user.getUserRole() == UserRole.SVIP)
                    && user.getMembershipEndTime() != null
                    && user.getMembershipEndTime().isAfter(LocalDateTime.now());
        } catch (Exception e) {
            log.error("检查会员状态失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> updateAvatar(Long userId, String avatarUrl) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 更新头像URL
            user.setAvatar(avatarUrl);
            userRepository.save(user);

            return ApiResponse.success("头像更新成功", user);
        } catch (Exception e) {
            log.error("更新用户头像失败", e);
            return ApiResponse.error("更新头像失败: " + e.getMessage());
        }
    }
}