package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.ForumTopic;
import com.zhixiang.knowledge_platform.entity.ForumTopicCollection;
import com.zhixiang.knowledge_platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ForumTopicCollectionRepository extends JpaRepository<ForumTopicCollection, Long> {
    boolean existsByTopicAndUser(ForumTopic topic, User user);
    void deleteByTopicAndUser(ForumTopic topic, User user);
    Page<ForumTopicCollection> findByUser(User user, Pageable pageable);
    List<ForumTopicCollection> findByUserAndTopicIn(User user, List<ForumTopic> topics);
}
