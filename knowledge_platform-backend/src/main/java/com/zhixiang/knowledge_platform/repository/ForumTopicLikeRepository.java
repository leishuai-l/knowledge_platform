package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.ForumTopicLike;
import com.zhixiang.knowledge_platform.entity.ForumTopic;
import com.zhixiang.knowledge_platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ForumTopicLikeRepository extends JpaRepository<ForumTopicLike, Long> {
    Optional<ForumTopicLike> findByTopicAndUser(ForumTopic topic, User user);
    boolean existsByTopicAndUser(ForumTopic topic, User user);
    void deleteByTopicAndUser(ForumTopic topic, User user);
    List<ForumTopicLike> findByUserAndTopicIn(User user, List<ForumTopic> topics);
}
