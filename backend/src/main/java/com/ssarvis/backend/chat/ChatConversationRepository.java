package com.ssarvis.backend.chat;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Optional<ChatConversation> findByIdAndUserId(Long id, Long userId);

    List<ChatConversation> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select new com.ssarvis.backend.chat.ChatConversationSummaryRow(
                conversation.id,
                conversation.promptGenerationLog.id,
                conversation.promptGenerationLog.alias,
                conversation.createdAt,
                (
                    select message.content
                    from ChatMessage message
                    where message.id = (
                        select max(lastMessage.id)
                        from ChatMessage lastMessage
                        where lastMessage.conversation.id = conversation.id
                    )
                ),
                (
                    select count(messageCount)
                    from ChatMessage messageCount
                    where messageCount.conversation.id = conversation.id
                )
            )
            from ChatConversation conversation
            where conversation.user.id = :userId
            order by conversation.createdAt desc
            """)
    List<ChatConversationSummaryRow> findSummaryRowsByUserId(@Param("userId") Long userId);
}
