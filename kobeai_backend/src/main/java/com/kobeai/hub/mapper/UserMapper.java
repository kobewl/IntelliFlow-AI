package com.kobeai.hub.mapper;

import com.kobeai.hub.dto.UserDTO;
import com.kobeai.hub.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * 将User实体转换为UserDTO
     * 
     * @param user User实体
     * @return UserDTO对象
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "avatar", source = "avatar")
    UserDTO toDTO(User user);

    /**
     * 将UserDTO转换为User实体
     * 
     * @param userDTO UserDTO对象
     * @return User实体
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "avatar", source = "avatar")
    User toEntity(UserDTO userDTO);
}