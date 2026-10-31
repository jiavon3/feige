package feige.repo;

import feige.domain.Conversation;
import feige.domain.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findFirstByType(ConversationType type);

    List<Conversation> findByType(ConversationType type);

    @Query("select distinct c from Conversation c, ConversationMember m "
            + "where m.conversation = c and m.user.id = :userId and c.type = feige.domain.ConversationType.DIRECT")
    List<Conversation> findDirectByUser(@Param("userId") long userId);

    @Query("select c from Conversation c where c.type = feige.domain.ConversationType.DIRECT "
            + "and exists (select m1.id from ConversationMember m1 where m1.conversation = c and m1.user.id = :low) "
            + "and exists (select m2.id from ConversationMember m2 where m2.conversation = c and m2.user.id = :high)")
    List<Conversation> findDirectBetween(@Param("low") long low, @Param("high") long high);
}
