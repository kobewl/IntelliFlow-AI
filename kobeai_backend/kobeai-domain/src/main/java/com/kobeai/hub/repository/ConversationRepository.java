package com.kobeai.hub.repository;

import com.kobeai.hub.model.Conversation;
import com.kobeai.hub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Conversation> findFirstByUserOrderByCreatedAtDesc(User user);

    List<Conversation> findByUserOrderByCreatedAtDesc(User user);
}