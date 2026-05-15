package com.kobeai.hub.constant;

/**
 * 模板相关常量
 */
public final class TemplateConstants {

    private TemplateConstants() {
        // 私有构造函数，防止实例化
    }

    /**
     * 评分相关常量
     */
    public static final class Score {
        public static final int MAX_TOKEN_MATCH_SCORE = 100;
        public static final int CODE_FEATURE_SCORE = 50;
        public static final int STRATEGY_MATCH_SCORE = 30;
        public static final double KEYWORD_MATCH_WEIGHT = 0.8;
    }

    /**
     * 压缩策略类型
     */
    public static final class CompressionType {
        public static final String SEMANTIC = "semantic";
        public static final String KEYWORD = "keyword";
        public static final String REDUNDANCY = "redundancy";
    }

    /**
     * 模板类型
     */
    public static final class TemplateType {
        public static final String CODE = "code";
        public static final String CHAT = "chat";
        public static final String SQL = "sql";
        public static final String DEFAULT = "default";
    }

    /**
     * Token 相关常量
     */
    public static final class TokenConfig {
        public static final int CODE_TEMPLATE_TOKENS = 200;
        public static final int CHAT_TEMPLATE_TOKENS = 100;
        public static final int SQL_TEMPLATE_TOKENS = 150;
        public static final int DEFAULT_TEMPLATE_TOKENS = 100;
    }

    /**
     * 正则表达式模式
     */
    public static final class Patterns {
        public static final String CODE_PATTERN = "(class|function|def|public|private|interface|impl|async|await|import|export)\\s+\\w+";
        public static final String SQL_PATTERN = "(?i)(SELECT|INSERT INTO|UPDATE|DELETE FROM|CREATE TABLE|ALTER TABLE|DROP TABLE|JOIN|WHERE|GROUP BY|ORDER BY|VALUES|SET|FROM|INTO|CRUD|MySQL|SQL)\\b";
        public static final String WORD_PATTERN = "\\b\\w+\\b";
    }

    /**
     * 描述文本
     */
    public static final class Description {
        public static final String CODE_TEMPLATE = "代码相关的模板";
        public static final String CHAT_TEMPLATE = "通用对话模板";
        public static final String SQL_TEMPLATE = "SQL相关的模板";
        public static final String DEFAULT_TEMPLATE = "默认模板";
    }
}