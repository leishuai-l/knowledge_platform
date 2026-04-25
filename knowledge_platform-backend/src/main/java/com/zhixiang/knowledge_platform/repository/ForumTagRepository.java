package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.ForumTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ForumTagRepository extends JpaRepository<ForumTag, Long> {
    Optional<ForumTag> findByName(String name);
}
