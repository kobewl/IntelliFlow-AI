package com.kobeai.hub.service.impl;

import com.kobeai.hub.dto.UserDTO;
import com.kobeai.hub.dto.request.RegisterRequest;
import com.kobeai.hub.dto.request.UserRequest;
import com.kobeai.hub.dto.request.UserUpdateRequest;
import com.kobeai.hub.dto.response.ApiResponse;
import com.kobeai.hub.exception.ResourceNotFoundException;
import com.kobeai.hub.mapper.UserMapper;
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
import java.util.concurrent.TimeUnit;

import com.kobeai.hub.constant.RedisKeyConstant;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Random;
import com.kobeai.hub.mq.EmailVerificationConsumer.EmailVerificationMessage;
import com.kobeai.hub.config.RabbitMQConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public ApiResponse<?> login(String username, String password) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ApiResponse.error("密码错误");
            }

            String token = jwtUtil.generateToken(user);

            // 将用户信息存入Redis缓存
            String redisKey = RedisKeyConstant.USER_INFO_KEY + user.getId();
            UserDTO userDTO = userMapper.toDTO(user);
            redisTemplate.opsForValue().set(redisKey, userDTO, RedisKeyConstant.USER_INFO_TTL, TimeUnit.SECONDS);

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
            if (userId == null) {
                return ApiResponse.error("用户ID不能为空");
            }
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                return ApiResponse.error("头像URL不能为空");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 更新头像URL
            user.setAvatar(avatarUrl.trim());
            User savedUser = userRepository.save(user);

            // 清除缓存
            String redisKey = RedisKeyConstant.USER_INFO_KEY + savedUser.getId();
            redisTemplate.delete(redisKey);

            // 只返回头像URL
            return ApiResponse.success("头像更新成功", savedUser.getAvatar());
        } catch (Exception e) {
            log.error("更新用户头像失败: {}", e.getMessage(), e);
            return ApiResponse.error("更新头像失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> updateProfile(User updatedUser) {
        try {
            if (updatedUser.getId() == null) {
                return ApiResponse.error("用户ID不能为空");
            }

            User existingUser = userRepository.findById(updatedUser.getId())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 用户名更新和验证
            if (updatedUser.getUsername() != null && !updatedUser.getUsername().equals(existingUser.getUsername())) {
                userRepository.findByUsername(updatedUser.getUsername())
                        .ifPresent(user -> {
                            if (!user.getId().equals(existingUser.getId())) {
                                throw new RuntimeException("用户名已被使用");
                            }
                        });
                existingUser.setUsername(updatedUser.getUsername());
            }

            // 邮箱更新和验证
            if (updatedUser.getEmail() != null && !updatedUser.getEmail().equals(existingUser.getEmail())) {
                userRepository.findByEmail(updatedUser.getEmail())
                        .ifPresent(user -> {
                            if (!user.getId().equals(existingUser.getId())) {
                                throw new RuntimeException("邮箱已被使用");
                            }
                        });
                existingUser.setEmail(updatedUser.getEmail());
            }

            // 手机号更新和验证
            if (updatedUser.getPhone() != null && !updatedUser.getPhone().equals(existingUser.getPhone())) {
                userRepository.findByPhone(updatedUser.getPhone())
                        .ifPresent(user -> {
                            if (!user.getId().equals(existingUser.getId())) {
                                throw new RuntimeException("手机号已被使用");
                            }
                        });
                existingUser.setPhone(updatedUser.getPhone());
            }

            // 更新其他基本信息
            if (updatedUser.getGender() != null) {
                existingUser.setGender(updatedUser.getGender());
            }
            if (updatedUser.getBio() != null) {
                existingUser.setBio(updatedUser.getBio());
            }
            if (updatedUser.getAvatar() != null) {
                existingUser.setAvatar(updatedUser.getAvatar());
            }

            // 保持原有的敏感信息不变
            existingUser.setUserRole(existingUser.getUserRole());
            existingUser.setMembershipStartTime(existingUser.getMembershipStartTime());
            existingUser.setMembershipEndTime(existingUser.getMembershipEndTime());

            // 保存更新
            User savedUser = userRepository.save(existingUser);

            // 清除缓存
            String redisKey = RedisKeyConstant.USER_INFO_KEY + savedUser.getId();
            redisTemplate.delete(redisKey);

            return ApiResponse.success("用户信息更新成功", savedUser);
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<?> changePassword(Long userId, String currentPassword, String newPassword) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 验证当前密码
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ApiResponse.error("当前密码错误");
            }

            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return ApiResponse.success("密码修改成功");
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserDTO findById(Long id) {
        // 从Redis获取缓存
        String redisKey = RedisKeyConstant.USER_INFO_KEY + id;
        UserDTO cachedUser = (UserDTO) redisTemplate.opsForValue().get(redisKey);
        if (cachedUser != null) {
            return cachedUser;
        }

        // 缓存不存在，从数据库查询
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserDTO userDTO = userMapper.toDTO(user);

        // 存入缓存
        redisTemplate.opsForValue().set(redisKey, userDTO, RedisKeyConstant.USER_INFO_TTL, TimeUnit.SECONDS);
        return userDTO;
    }

    @Override
    @CacheEvict(value = "users", key = "#id")
    public User updateUser(Long id, UserUpdateRequest request) {
        // 更新用户信息
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 更新用户信息
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getUserRole() != null) {
            user.setUserRole(request.getUserRole());
        }
        if (request.getMembershipEndTime() != null) {
            user.setMembershipEndTime(request.getMembershipEndTime());
        }

        // 保存更新
        user = userRepository.save(user);

        // 删除缓存
        String redisKey = RedisKeyConstant.USER_INFO_KEY + id;
        redisTemplate.delete(redisKey);

        return user;
    }

    @Override
    public Page<User> getUserList(String username, String role, Pageable pageable) {
        // 如果没有提供搜索条件，返回所有用户
        if (username == null && role == null) {
            return userRepository.findAll(pageable);
        }

        // 如果提供了用户名和角色
        if (username != null && role != null) {
            return userRepository.findByUsernameContainingAndUserRole(username, UserRole.valueOf(role), pageable);
        }

        // 如果只提供了用户名
        if (username != null) {
            return userRepository.findByUsernameContaining(username, pageable);
        }

        // 如果只提供了角色
        return userRepository.findByUserRole(UserRole.valueOf(role), pageable);
    }

    @Override
    public User addUser(UserRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());
        user.setUserRole(request.getUserRole());
        user.setMembershipEndTime(request.getMembershipEndTime());

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        userRepository.delete(user);
    }

    @Override
    public User setUserRole(Long id, UserRole role, LocalDateTime membershipEndTime) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setUserRole(role);
        user.setMembershipEndTime(membershipEndTime);

        return userRepository.save(user);
    }

    @Override
    public long count() {
        return userRepository.count();
    }

    @Override
    public long countByCreatedAtAfter(LocalDateTime dateTime) {
        return userRepository.countByCreatedAtAfter(dateTime);
    }

    @Override
    public long countByUserRole(UserRole role) {
        return userRepository.countByUserRole(role);
    }

    @Override
    public ApiResponse<?> sendEmailVerificationCode(String email) {
        try {
            // 生成6位随机验证码
            String verificationCode = generateVerificationCode();

            // 将验证码保存到Redis，设置5分钟过期
            String redisKey = "email:verification:" + email;
            try {
                redisTemplate.opsForValue().set(redisKey, verificationCode, 5, TimeUnit.MINUTES);
                log.info("Saving verification code to Redis. Key: {}, Code: {}", redisKey, verificationCode);

                // 立即读取验证一下是否保存成功
                String savedCode = (String) redisTemplate.opsForValue().get(redisKey);
                log.info("Immediately checking saved code in Redis. Key: {}, Saved Code: {}, Expected Code: {}",
                        redisKey, savedCode, verificationCode);

                if (savedCode == null) {
                    log.error("Failed to save verification code to Redis. Code was not found after saving.");
                    return ApiResponse.error("验证码保存失败");
                }
            } catch (Exception e) {
                log.error("Redis operation failed: {}", e.getMessage(), e);
                return ApiResponse.error("Redis操作失败: " + e.getMessage());
            }

            // 创建消息对象
            EmailVerificationMessage message = new EmailVerificationMessage();
            message.setEmail(email);
            message.setCode(verificationCode);

            // 发送到RabbitMQ队列
            rabbitTemplate.convertAndSend("email.verification.queue", message);
            log.info("Verification code sent to MQ for email: {}", email);

            return ApiResponse.success("验证码已发送到邮箱");
        } catch (Exception e) {
            log.error("Failed to send verification code: {}", e.getMessage(), e);
            return ApiResponse.error("发送验证码失败: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> verifyEmailCode(String email, String code) {
        String redisKey = "email:verification:" + email;
        try {
            String savedCode = (String) redisTemplate.opsForValue().get(redisKey);
            log.info("Verifying code. Key: {}, Input Code: {}, Saved Code: {}", redisKey, code, savedCode);

            if (savedCode == null) {
                log.warn("Verification code not found in Redis for key: {}", redisKey);
                return ApiResponse.error("验证码已过期");
            }

            if (!savedCode.equals(code)) {
                log.warn("Code mismatch. Input: {}, Saved: {}", code, savedCode);
                return ApiResponse.error("验证码错误");
            }

            // 验证成功后删除验证码
            Boolean deleteResult = redisTemplate.delete(redisKey);
            log.info("Delete verification code from Redis. Key: {}, Result: {}", redisKey, deleteResult);

            return ApiResponse.success("验证成功");
        } catch (Exception e) {
            log.error("Redis verification operation failed: {}", e.getMessage(), e);
            return ApiResponse.error("验证码验证失败: " + e.getMessage());
        }
    }

    // 生成6位随机验证码
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}