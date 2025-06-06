package com.kobeai.hub.repository;

import com.kobeai.hub.model.Conversation;
import com.kobeai.hub.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

        /**
         * 获取会话的最近消息
         */
        @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC")
        List<Message> findMessages(@Param("conversationId") Long conversationId, Pageable pageable);

        /**
         * 获取会话在指定时间之前的消息
         */
        @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.createdAt < :time ORDER BY m.createdAt ASC")
        List<Message> findMessagesBefore(
                        @Param("conversationId") Long conversationId,
                        @Param("time") LocalDateTime time,
                        Pageable pageable);

        /**
         * 删除指定会话的所有消息
         */
        @Modifying
        @Query("DELETE FROM Message m WHERE m.conversation.id = :conversationId")
        void deleteByConversationId(Long conversationId);

        /**
         * 获取会话的最近N条消息
         */
        @Query(value = "SELECT * FROM messages WHERE conversation_id = :conversationId ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
        List<Message> findRecentMessages(Long conversationId, int limit);

        /**
         * 根据会话对象获取消息，按创建时间升序排序
         */
        @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt ASC")
        List<Message> findByConversation(@Param("conversation") Conversation conversation, Pageable pageable);

        /**
         * 根据会话对象获取指定时间之前的消息
         */
        @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.createdAt < :time ORDER BY m.createdAt ASC")
        List<Message> findByConversationAndCreatedAtBefore(
                        @Param("conversation") Conversation conversation,
                        @Param("time") LocalDateTime time,
                        Pageable pageable);

        /**
         * 根据会话ID和时间戳查询之前的消息，按创建时间降序排序
         */
        List<Message> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                        Long conversationId, LocalDateTime timestamp, Pageable pageable);

        /**
         * 根据会话ID查询最新消息，按创建时间降序排序
         */
        List<Message> findByConversationIdOrderByCreatedAtDesc(
                        Long conversationId, Pageable pageable);

        /**
         * 根据会话统计消息数量
         */
        long countByConversation(Conversation conversation);
}