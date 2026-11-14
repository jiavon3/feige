package feige.repo;

import feige.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversation_IdIn(Collection<Long> conversationIds);

    @Query("select m.storedName from ChatMessage m where m.conversation.id = :conversationId "
            + "and m.storedName is not null")
    List<String> findStoredNamesByConversationId(@Param("conversationId") long conversationId);

    @Modifying
    @Query("delete from ChatMessage m where m.conversation.id = :conversationId")
    void deleteByConversationId(@Param("conversationId") long conversationId);

    @Query("select m from ChatMessage m where m.sender.id = :senderId "
            + "and m.conversation.type = feige.domain.ConversationType.HALL")
    List<ChatMessage> findHallMessagesBySender(@Param("senderId") long senderId);

    @Query("select m from ChatMessage m where m.conversation.id = :conversationId "
            + "order by m.id desc")
    List<ChatMessage> findLatest(@Param("conversationId") long conversationId, Pageable pageable);

    @Query("select m from ChatMessage m where m.conversation.id = :conversationId "
            + "and m.id < :beforeId order by m.id desc")
    List<ChatMessage> findBefore(@Param("conversationId") long conversationId,
                                @Param("beforeId") long beforeId,
                                Pageable pageable);

    Optional<ChatMessage> findFirstByConversation_IdOrderByIdDesc(Long conversationId);

    @Query("select count(m) from ChatMessage m where m.conversation.id = :conversationId "
            + "and m.sender.id <> :userId and m.recalled = false")
    long countUnreadAll(@Param("conversationId") long conversationId,
                        @Param("userId") long userId);

    @Query("select count(m) from ChatMessage m where m.conversation.id = :conversationId "
            + "and m.sender.id <> :userId and m.recalled = false and m.id > :afterId")
    long countUnreadAfter(@Param("conversationId") long conversationId,
                          @Param("userId") long userId,
                          @Param("afterId") long afterId);
}
