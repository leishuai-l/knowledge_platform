package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.ForumReplyLike;
import com.zhixiang.knowledge_platform.entity.ForumReply;
import com.zhixiang.knowledge_platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ForumReplyLikeRepository extends JpaRepository<ForumReplyLike, Long> {
    Optional<ForumReplyLike> findByReplyAndUser(ForumReply reply, User user);
    boolean existsByReplyAndUser(ForumReply reply, User user);
    void deleteByReplyAndUser(ForumReply reply, User user);
}
