
create database if not exists IntelliFlowAI;

use IntelliFlowAI;

-- 用户表
create table users
(
    id                    bigint auto_increment
        primary key,
    created_at            datetime(6)                   null,
    email                 varchar(255)                  null,
    password              varchar(255)                  not null,
    updated_at            datetime(6)                   null,
    username              varchar(255)                  not null,
    phone                 varchar(255)                  null,
    membership_end_time   datetime(6)                   null,
    membership_start_time datetime(6)                   null,
    user_role             varchar(255)                  not null,
    is_deleted            tinyint(1)  default 0         not null comment '0-未删除 1-已删除',
    avatar                varchar(255)                  null comment '用户头像URL',
    bio                   text                          null comment '个人简介',
    gender                varchar(10) default 'UNKNOWN' null comment '性别：MALE-男 FEMALE-女 UNKNOWN-未知',
    constraint UK_du5v5sr43g5bfnji4vb8hg5s3
        unique (phone),
    constraint UK_r43af9ap4edm43mmtq01oddj6
        unique (username)
)
    charset = utf8mb4;

-- 系统通知表
create table system_notifications
(
    id         bigint auto_increment
        primary key,
    content    text         not null,
    created_at datetime(6)  null,
    created_by bigint       not null,
    status     varchar(255) not null,
    title      varchar(255) not null,
    type       varchar(255) not null,
    updated_at datetime(6)  null
)
    charset = utf8mb4;

-- 提示模板表
create table prompt_templates
(
    id                   bigint auto_increment
        primary key,
    compression_strategy varchar(255) null,
    content              text         not null,
    created_at           datetime(6)  not null,
    description          varchar(255) null,
    estimated_tokens     int          null,
    name                 varchar(255) not null,
    type                 varchar(255) not null,
    updated_at           datetime(6)  null,
    variables            text         null,
    average_score        double       null,
    usage_count          int          null
)
    charset = utf8mb4;

-- 通知表
create table notifications
(
    id         bigint auto_increment
        primary key,
    content    text         null,
    created_at datetime(6)  null,
    status     varchar(255) not null,
    title      varchar(255) not null,
    type       varchar(255) not null,
    updated_at datetime(6)  null
)
    charset = utf8mb4;

-- 消息表
create table messages
(
    id              bigint auto_increment
        primary key,
    content         text                 null,
    created_at      datetime(6)          null,
    role            varchar(255)         null,
    conversation_id bigint               not null,
    sender_id       bigint               not null,
    is_deleted      tinyint(1) default 0 not null,
    updated_at      datetime(6)          null,
    constraint FKt492th6wsovh1nush5yl5jj8e
        foreign key (conversation_id) references conversations (id)
)
    charset = utf8mb4;

-- 会话表
create table conversations
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)  null,
    title       varchar(255) null,
    platform_id bigint       not null,
    user_id     bigint       not null,
    constraint FK8uurqd0mfocwdvi6t3wkclmdt
        foreign key (platform_id) references ai_platforms (id),
    constraint FKpltqvfcbkql9svdqwh0hw4g1d
        foreign key (user_id) references users (id)
)
    charset = utf8mb4;


-- AI平台表
create table ai_platforms
(
    id          bigint auto_increment
        primary key,
    api_key     varchar(255)               null,
    base_url    varchar(255)               null,
    created_at  datetime(6)                null,
    enabled     bit                        not null,
    name        varchar(255)               not null,
    type        varchar(255)               not null,
    description varchar(255)               not null,
    user_id     bigint                     null comment '0代表系统AI，其他数字代表用户添加的AI',
    model       varchar(255)               null,
    max_tokens  int           default 2000 null,
    temperature decimal(3, 2) default 0.70 null
)
    charset = utf8mb4;

create index fk_user_id
    on ai_platforms (user_id);

