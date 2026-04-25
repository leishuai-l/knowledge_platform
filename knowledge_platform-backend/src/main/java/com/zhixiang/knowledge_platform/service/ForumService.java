package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.request.*;
import com.zhixiang.knowledge_platform.dto.response.*;
import com.zhixiang.knowledge_platform.entity.*;
import com.zhixiang.knowledge_platform.exception.ResourceNotFoundException;
import com.zhixiang.knowledge_platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForumService {

    private final ForumCategoryRepository categoryRepository;
    private final ForumTopicRepository topicRepository;
    private final ForumReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final ForumTagRepository tagRepository;
    private final ForumTopicLikeRepository topicLikeRepository;
    private final ForumReplyLikeRepository replyLikeRepository;
    private final ForumTopicCollectionRepository collectionRepository;
    private final UserFollowRepository userFollowRepository;

    public List<ForumCategoryResponse> getAllCategories() {
        List<ForumCategory> allCategories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"));
        log.info("Found {} total categories", allCategories.size());
        
        List<ForumCategoryResponse> activeCategories = allCategories.stream()
                .filter(c -> {
                    boolean isActive = Boolean.TRUE.equals(c.getStatus());
                    if (!isActive) {
                        log.info("Category {} (id={}) is inactive (status={})", c.getName(), c.getId(), c.getStatus());
                    }
                    return isActive;
                })
                .map(ForumCategoryResponse::fromEntity)
                .collect(Collectors.toList());
                
        log.info("Returning {} active categories", activeCategories.size());
        return activeCategories;
    }

    @Transactional
    public boolean toggleTopicCollection(Long userId, Long topicId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        if (collectionRepository.existsByTopicAndUser(topic, user)) {
            collectionRepository.deleteByTopicAndUser(topic, user);
            int currentCount = topic.getCollectionCount() != null ? topic.getCollectionCount() : 0;
            topic.setCollectionCount(Math.max(0, currentCount - 1));
            topicRepository.save(topic);
            return false;
        } else {
            ForumTopicCollection collection = new ForumTopicCollection();
            collection.setTopic(topic);
            collection.setUser(user);
            collectionRepository.save(collection);
            int currentCount = topic.getCollectionCount() != null ? topic.getCollectionCount() : 0;
            topic.setCollectionCount(currentCount + 1);
            topicRepository.save(topic);
            return true;
        }
    }

    @Transactional
    public boolean toggleUserFollow(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        User followed = userRepository.findById(followedId)
                .orElseThrow(() -> new ResourceNotFoundException("关注的用户不存在"));

        if (userFollowRepository.existsByFollowerAndFollowed(follower, followed)) {
            userFollowRepository.deleteByFollowerAndFollowed(follower, followed);
            return false;
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollower(follower);
            follow.setFollowed(followed);
            userFollowRepository.save(follow);
            return true;
        }
    }
    
    @Transactional(readOnly = true)
    public PageResponse<ForumTopicResponse> getUserCollections(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumTopicCollection> collectionPage = collectionRepository.findByUser(user, pageable);
        
        List<ForumTopicResponse> list = collectionPage.getContent().stream()
            .map(collection -> {
                ForumTopicResponse response = ForumTopicResponse.fromEntity(collection.getTopic(), userId);
                response.setIsCollected(true);
                response.setIsLiked(topicLikeRepository.existsByTopicAndUser(collection.getTopic(), user));
                return response;
            })
            .collect(Collectors.toList());
            
        return PageResponse.fromPage(collectionPage, list);
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumTopicResponse> getUserDrafts(Long userId, int page, int size) {
        // Assuming drafts are topics with status = 3
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ForumTopic> draftPage = topicRepository.findByAuthorIdAndStatus(userId, 3, pageable);
        
        List<ForumTopicResponse> list = draftPage.getContent().stream()
            .map(topic -> ForumTopicResponse.fromEntity(topic, userId))
            .collect(Collectors.toList());
            
        return PageResponse.fromPage(draftPage, list);
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumTopicResponse> getTopics(Long categoryId, String tag, Integer status, String sort, int page, int size, Long currentUserId) {
        // Page index starts from 0 in Spring Data
        Sort sortObj;
        if ("hot".equals(sort)) {
            // Sort by replyCount and viewCount descending for hot topics
            sortObj = Sort.by(Sort.Direction.DESC, "isPinned", "replyCount", "viewCount", "createdAt");
        } else {
            // Default to latest
            sortObj = Sort.by(Sort.Direction.DESC, "isPinned", "createdAt");
        }
        
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<ForumTopic> topicPage;

        int queryStatus = status != null ? status : 0; // Default to normal topics

        log.info("Querying topics - categoryId: {}, tag: {}, status: {}, sort: {}, page: {}, size: {}", categoryId, tag, queryStatus, sort, page, size);

        if (categoryId != null && tag != null && !tag.isEmpty()) {
            topicPage = topicRepository.findByCategoryIdAndTagsNameAndStatus(categoryId, tag, queryStatus, pageable);
        } else if (categoryId != null) {
            topicPage = topicRepository.findByCategoryIdAndStatus(categoryId, queryStatus, pageable);
        } else if (tag != null && !tag.isEmpty()) {
            topicPage = topicRepository.findByTagsNameAndStatus(tag, queryStatus, pageable);
        } else {
            topicPage = topicRepository.findByStatus(queryStatus, pageable);
        }

        log.info("Found {} topics", topicPage.getTotalElements());

        // Batch fetch likes and collections to avoid N+1 queries
        java.util.Set<Long> likedTopicIds = new java.util.HashSet<>();
        java.util.Set<Long> collectedTopicIds = new java.util.HashSet<>();
        if (currentUserId != null) {
            User currentUser = userRepository.getReferenceById(currentUserId);
            List<ForumTopic> topics = topicPage.getContent();
            if (!topics.isEmpty()) {
                List<ForumTopicLike> likes = topicLikeRepository.findByUserAndTopicIn(currentUser, topics);
                likes.forEach(like -> likedTopicIds.add(like.getTopic().getId()));
                List<ForumTopicCollection> collections = collectionRepository.findByUserAndTopicIn(currentUser, topics);
                collections.forEach(col -> collectedTopicIds.add(col.getTopic().getId()));
            }
        }

        // Use fromPage without mapping first, then map content to ensure correct generic type inference
        PageResponse<ForumTopicResponse> response = new PageResponse<>();
        List<ForumTopicResponse> list = topicPage.getContent().stream()
            .map(topic -> {
                try {
                    ForumTopicResponse tr = ForumTopicResponse.fromEntity(topic, currentUserId);
                    if (currentUserId != null) {
                        tr.setIsLiked(likedTopicIds.contains(topic.getId()));
                        tr.setIsCollected(collectedTopicIds.contains(topic.getId()));
                    }
                    return tr;
                } catch (Exception e) {
                    log.error("Error converting topic to response: {}", topic.getId(), e);
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
            
        response.setList(list);
        response.setTotal(topicPage.getTotalElements());
        response.setPage(topicPage.getNumber() + 1);
        response.setSize(topicPage.getSize());
        response.setPages(topicPage.getTotalPages());
        response.setIsFirst(topicPage.isFirst());
        response.setIsLast(topicPage.isLast());
        response.setHasPrevious(topicPage.hasPrevious());
        response.setHasNext(topicPage.hasNext());
        
        return response;
    }

    @Transactional
    public ForumTopicResponse getTopicDetail(Long id, Long currentUserId) {
        ForumTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        
        // Increment view count
        topic.setViewCount(topic.getViewCount() + 1);
        topicRepository.save(topic);
        
        ForumTopicResponse response = ForumTopicResponse.fromEntity(topic, currentUserId);
        if (currentUserId != null) {
            response.setIsLiked(topicLikeRepository.existsByTopicAndUser(topic, userRepository.getReferenceById(currentUserId)));
            response.setIsCollected(collectionRepository.existsByTopicAndUser(topic, userRepository.getReferenceById(currentUserId)));
        }
        return response;
    }

    @Transactional
    public ForumTopicResponse createTopic(Long userId, ForumTopicCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        ForumCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("板块不存在"));

        ForumTopic topic = new ForumTopic();
        topic.setTitle(request.getTitle());
        topic.setContent(request.getContent());
        topic.setAuthor(user);
        topic.setCategory(category);
        topic.setViewCount(0);
        topic.setReplyCount(0);
        topic.setIsPinned(false);
        topic.setIsEssence(false);
        // Use status from request, default to 0 (Normal)
        topic.setStatus(request.getStatus() != null ? request.getStatus() : 0);
        
        // Handle tags
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<ForumTag> forumTags = new HashSet<>();
            for (String tagName : request.getTags()) {
                ForumTag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        ForumTag newTag = new ForumTag();
                        newTag.setName(tagName);
                        newTag.setTopicCount(0);
                        return tagRepository.save(newTag);
                    });
                tag.setTopicCount(tag.getTopicCount() + 1);
                forumTags.add(tag);
            }
            topic.setTags(forumTags);
        }
        
        ForumTopic savedTopic = topicRepository.save(topic);
        
        // Force flush to ensure DB triggers run and IDs are generated
        topicRepository.flush();
        
        // Refresh to get any DB-generated defaults
        // savedTopic = topicRepository.findById(savedTopic.getId()).orElse(savedTopic);
        
        log.info("Created new topic: id={}, title={}, status={}", savedTopic.getId(), savedTopic.getTitle(), savedTopic.getStatus());
        
        return ForumTopicResponse.fromEntity(savedTopic);
    }

    @Transactional
    public ForumReplyResponse createReply(Long userId, ForumReplyCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        ForumTopic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        ForumReply reply = new ForumReply();
        reply.setContent(request.getContent());
        reply.setAuthor(user);
        reply.setTopic(topic);
        reply.setStatus(0); // Normal

        ForumReply savedReply = replyRepository.save(reply);
        
        // Update topic reply count
        topic.setReplyCount(topic.getReplyCount() + 1);
        topicRepository.save(topic);

        return ForumReplyResponse.fromEntity(savedReply);
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumReplyResponse> getReplies(Long topicId, int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ForumReply> replyPage = replyRepository.findByTopicId(topicId, pageable);
        
        PageResponse<ForumReplyResponse> response = new PageResponse<>();
        response.setList(replyPage.getContent().stream().map(reply -> {
            ForumReplyResponse rr = ForumReplyResponse.fromEntity(reply, currentUserId);
            if (currentUserId != null) {
                rr.setIsLiked(replyLikeRepository.existsByReplyAndUser(reply, userRepository.getReferenceById(currentUserId)));
            }
            return rr;
        }).collect(Collectors.toList()));
        response.setTotal(replyPage.getTotalElements());
        response.setPage(replyPage.getNumber() + 1);
        response.setSize(replyPage.getSize());
        response.setPages(replyPage.getTotalPages());
        response.setIsFirst(replyPage.isFirst());
        response.setIsLast(replyPage.isLast());
        response.setHasPrevious(replyPage.hasPrevious());
        response.setHasNext(replyPage.hasNext());
        
        return response;
    }

    @Transactional
    public boolean toggleTopicLike(Long userId, Long topicId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));

        if (topicLikeRepository.existsByTopicAndUser(topic, user)) {
            topicLikeRepository.deleteByTopicAndUser(topic, user);
            topicRepository.decrementLikeCount(topicId);
            return false;
        } else {
            ForumTopicLike like = new ForumTopicLike();
            like.setTopic(topic);
            like.setUser(user);
            topicLikeRepository.save(like);
            topicRepository.incrementLikeCount(topicId);
            return true;
        }
    }

    @Transactional
    public boolean toggleReplyLike(Long userId, Long replyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        ForumReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new ResourceNotFoundException("回复不存在"));

        if (replyLikeRepository.existsByReplyAndUser(reply, user)) {
            replyLikeRepository.deleteByReplyAndUser(reply, user);
            int currentCount = reply.getLikeCount() != null ? reply.getLikeCount() : 0;
            reply.setLikeCount(Math.max(0, currentCount - 1));
            replyRepository.save(reply);
            return false;
        } else {
            ForumReplyLike like = new ForumReplyLike();
            like.setReply(reply);
            like.setUser(user);
            replyLikeRepository.save(like);
            int currentCount = reply.getLikeCount() != null ? reply.getLikeCount() : 0;
            reply.setLikeCount(currentCount + 1);
            replyRepository.save(reply);
            return true;
        }
    }

    @Transactional
    public boolean toggleTopicPin(Long topicId) {
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        
        boolean newStatus = !Boolean.TRUE.equals(topic.getIsPinned());
        topic.setIsPinned(newStatus);
        topicRepository.save(topic);
        return newStatus;
    }

    @Transactional
    public boolean toggleTopicEssence(Long topicId) {
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        
        boolean newStatus = !Boolean.TRUE.equals(topic.getIsEssence());
        topic.setIsEssence(newStatus);
        topicRepository.save(topic);
        return newStatus;
    }

    public List<ForumTagResponse> getAllTags() {
        List<ForumTagResponse> tags = tagRepository.findAll().stream()
                .map(ForumTagResponse::fromEntity)
                .collect(Collectors.toList());
        log.info("Found {} tags", tags.size());
        return tags;
    }

    @Transactional(readOnly = true)
    public List<ForumTopicResponse> getHotTopics(int limit) {
        // Hot topic algorithm: (viewCount * 1 + replyCount * 5 + likeCount * 10) / hoursSinceCreated
        // For simplicity, let's just use a weighted sum for now
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "replyCount", "viewCount"));
        List<ForumTopicResponse> topics = topicRepository.findByStatus(0, pageable)
                .getContent().stream()
                .map(ForumTopicResponse::fromEntity)
                .collect(Collectors.toList());
        log.info("Found {} hot topics with limit {}", topics.size(), limit);
        return topics;
    }

    // 管理员方法

    @Transactional
    public void toggleTopicStatus(Long topicId) {
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        topic.setStatus(topic.getStatus() == 0 ? 2 : 0);
        topicRepository.save(topic);
    }

    @Transactional
    public void deleteTopic(Long topicId) {
        topicRepository.deleteById(topicId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ForumReplyResponse> searchReplies(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumReply> replyPage = keyword != null && !keyword.isEmpty()
            ? replyRepository.findByContentContaining(keyword, pageable)
            : replyRepository.findAll(pageable);
        List<ForumReplyResponse> replies = replyPage.getContent().stream()
            .map(ForumReplyResponse::fromEntity)
            .collect(Collectors.toList());
        return PageResponse.fromPage(replyPage, replies);
    }

    @Transactional
    public void deleteReply(Long replyId) {
        replyRepository.deleteById(replyId);
    }

    @Transactional
    public ForumCategoryResponse createCategory(ForumCategoryResponse request) {
        ForumCategory category = new ForumCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setStatus(true);
        return ForumCategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public ForumCategoryResponse updateCategory(Long id, ForumCategoryResponse request) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("板块不存在"));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        return ForumCategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
