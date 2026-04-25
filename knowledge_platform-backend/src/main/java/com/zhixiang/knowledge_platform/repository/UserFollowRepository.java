package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.entity.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    boolean existsByFollowerAndFollowed(User follower, User followed);
    void deleteByFollowerAndFollowed(User follower, User followed);
    Page<UserFollow> findByFollower(User follower, Pageable pageable);
    Page<UserFollow> findByFollowed(User followed, Pageable pageable);
}
