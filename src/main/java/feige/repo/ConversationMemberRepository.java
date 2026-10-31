package feige.repo;

import feige.domain.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    boolean existsByConversation_IdAndUser_Id(Long conversationId, Long userId);

    Optional<ConversationMember> findByConversation_IdAndUser_Id(Long conversationId, Long userId);

    List<ConversationMember> findByConversation_IdIn(Collection<Long> conversationIds);

    List<ConversationMember> findByUser_Id(Long userId);

    @Query("select m.user.id from ConversationMember m where m.conversation.id = :conversationId")
    List<Long> findUserIdsByConversationId(@Param("conversationId") long conversationId);
}
