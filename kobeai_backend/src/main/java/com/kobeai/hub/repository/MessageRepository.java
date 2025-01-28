package com.kobeai.hub.repository;

import com.kobeai.hub.model.Conversation;
import com.kobeai.hub.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
        @Query("SELECT m FROM Message m WHERE m.conversation = :conversation ORDER BY m.createdAt ASC")
        List<Message> findByConversationOrderByCreatedAtDesc(
                        @Param("conversation") Conversation conversation,
                        Pageable pageable);

        @Query("SELECT m FROM Message m WHERE m.conversation = :conversation AND m.createdAt < :createdAt ORDER BY m.createdAt ASC")
        List<Message> findByConversationAndCreatedAtBeforeOrderByCreatedAtDesc(
                        @Param("conversation") Conversation conversation,
                        @Param("createdAt") LocalDateTime createdAt,
                        Pageable pageable);

        void deleteByConversation(Conversation conversation);
}