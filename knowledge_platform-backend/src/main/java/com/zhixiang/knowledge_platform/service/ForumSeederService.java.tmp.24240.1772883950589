package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.entity.*;
import com.zhixiang.knowledge_platform.enums.UserRole;
import com.zhixiang.knowledge_platform.enums.UserStatus;
import com.zhixiang.knowledge_platform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForumSeederService {

    private final ForumTopicRepository topicRepository;
    private final ForumReplyRepository replyRepository;
    private final ForumCategoryRepository categoryRepository;
    private final ForumTagRepository tagRepository;
    private final ForumTopicLikeRepository topicLikeRepository;
    private final ForumReplyLikeRepository replyLikeRepository;
    private final ForumTopicCollectionRepository collectionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void resetAndSeedForumData() {
        log.info("Starting forum data reset and seeding...");

        // 1. Clean up existing data
        cleanupData();

        // 2. Get or create admin user
        User admin = getOrCreateAdmin();
        User testUser = getOrCreateTestUser(); // Create a normal user for interaction

        // 3. Seed data for each category
        List<ForumCategory> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            log.warn("No categories found. Skipping seeding.");
            return;
        }

        // Prepare tags
        List<ForumTag> tags = createTags();

        for (ForumCategory category : categories) {
            seedTopicsForCategory(category, admin, testUser, tags);
        }

        log.info("Forum data reset and seeding completed successfully.");
    }

    private void cleanupData() {
        log.info("Cleaning up forum data...");
        replyLikeRepository.deleteAll();
        topicLikeRepository.deleteAll();
        collectionRepository.deleteAll();
        replyRepository.deleteAll();
        topicRepository.deleteAll();
        tagRepository.deleteAll();
        log.info("Forum data cleaned.");
    }

    private User getOrCreateAdmin() {
        return userRepository.findByRole(UserRole.ADMIN).stream().findFirst()
                .map(user -> {
                    // Update password if exists
                    user.setPassword(passwordEncoder.encode("123456"));
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername("admin");
                    user.setEmail("admin@example.com");
                    user.setPassword(passwordEncoder.encode("123456"));
                    user.setRole(UserRole.ADMIN);
                    user.setStatus(UserStatus.ACTIVE);
                    user.setPoints(1000);
                    user.setAvatar("avatars/default.png"); // Placeholder
                    return userRepository.save(user);
                });
    }

    private User getOrCreateTestUser() {
        return userRepository.findByUsername("test").orElseGet(() -> {
            User user = new User();
            user.setUsername("test");
            user.setEmail("test@example.com");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRole(UserRole.USER);
            user.setStatus(UserStatus.ACTIVE);
            user.setPoints(100);
            user.setAvatar("avatars/default.png"); // Placeholder
            return userRepository.save(user);
        });
    }

    private List<ForumTag> createTags() {
        String[] tagNames = {
            "Java", "Spring Boot", "Vue", "React", "MySQL", 
            "Redis", "架构设计", "面试题", "职场心得", "资源分享",
            "微服务", "Docker", "K8s", "DevOps", "AI",
            "Python", "Go", "前端", "后端", "全栈"
        };
        
        List<ForumTag> tags = new ArrayList<>();
        for (String name : tagNames) {
            ForumTag tag = new ForumTag();
            tag.setName(name);
            tag.setTopicCount(0);
            tags.add(tagRepository.save(tag));
        }
        return tags;
    }

    private void seedTopicsForCategory(ForumCategory category, User admin, User testUser, List<ForumTag> allTags) {
        log.info("Seeding topics for category: {}", category.getName());
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            ForumTopic topic = new ForumTopic();
            topic.setCategory(category);
            topic.setAuthor(admin);
            topic.setStatus(0); // Normal
            
            // Generate content based on category
            String title = generateTitle(category.getName(), i);
            String content = generateContent(category.getName(), title);
            
            topic.setTitle(title);
            topic.setContent(content);
            
            // Random stats
            topic.setViewCount(random.nextInt(1000));
            topic.setLikeCount(random.nextInt(100));
            topic.setCollectionCount(random.nextInt(50));
            topic.setIsPinned(random.nextInt(100) < 5); // 5% chance to be pinned
            topic.setIsEssence(random.nextInt(100) < 10); // 10% chance to be essence
            
            // Random tags
            Set<ForumTag> topicTags = new HashSet<>();
            int tagCount = random.nextInt(3) + 1; // 1-3 tags
            for (int j = 0; j < tagCount; j++) {
                ForumTag tag = allTags.get(random.nextInt(allTags.size()));
                topicTags.add(tag);
                // We'll update tag count later or let it be inconsistent for now (simplification)
                // Ideally should increment tag.topicCount
                tag.setTopicCount(tag.getTopicCount() + 1);
                tagRepository.save(tag);
            }
            topic.setTags(topicTags);
            
            ForumTopic savedTopic = topicRepository.save(topic);
            
            // Generate replies
            int replyCount = random.nextInt(6); // 0-5 replies
            for (int k = 0; k < replyCount; k++) {
                ForumReply reply = new ForumReply();
                reply.setTopic(savedTopic);
                reply.setAuthor(random.nextBoolean() ? admin : testUser);
                reply.setContent(generateReplyContent());
                reply.setStatus(0);
                reply.setLikeCount(random.nextInt(20));
                replyRepository.save(reply);
            }
            
            savedTopic.setReplyCount(replyCount);
            topicRepository.save(savedTopic);
        }
    }

    private String generateTitle(String categoryName, int index) {
        String[] prefixes = {"【强烈推荐】", "【求助】", "【讨论】", "【分享】", "【原创】", "【教程】"};
        String[] subjects = {
            "关于" + categoryName + "的一些思考",
            "如何高效进行" + categoryName,
            categoryName + "领域的最新趋势",
            "分享一个" + categoryName + "的小技巧",
            "大家在" + categoryName + "中遇到的最大困难是什么？",
            "推荐几个" + categoryName + "的好工具",
            "深入理解" + categoryName + "的核心概念",
            "从入门到精通：" + categoryName + "指南",
            categoryName + "常见误区总结",
            "我的" + categoryName + "学习之路"
        };
        
        Random random = new Random();
        return prefixes[random.nextInt(prefixes.length)] + subjects[index % subjects.length];
    }

    private String generateContent(String categoryName, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(title).append("\n\n");
        
        sb.append("大家好，今天想和大家聊聊关于 **").append(categoryName).append("** 的话题。\n\n");
        
        String[] paragraphs = {
            "在当今快速发展的技术领域，掌握核心技能显得尤为重要。我最近在项目中遇到了一些挑战，通过深入研究，我发现了一些有趣的解决方案。",
            "首先，我们需要明确目标。无论是为了职业发展，还是纯粹的兴趣爱好，清晰的规划都是成功的关键。我建议大家可以制定一个短期的学习计划。",
            "其次，实践出真知。理论知识固然重要，但没有实际操作，很难真正掌握。我尝试用最新的框架重构了一个模块，效果出乎意料的好。",
            "另外，工具的选择也很关键。一个好用的工具可以事半功倍。我最近在用的一些开源工具，极大地提高了我的工作效率。",
            "最后，保持持续学习的心态。技术更新迭代很快，我们要时刻保持敏感度。多关注社区动态，多和同行交流，是非常好的提升方式。"
        };
        
        Random random = new Random();
        for (int i = 0; i < 3 + random.nextInt(3); i++) {
            sb.append(paragraphs[random.nextInt(paragraphs.length)]).append("\n\n");
        }
        
        sb.append("### 代码示例\n\n");
        sb.append("```java\n");
        sb.append("public class Example {\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        System.out.println(\"Hello, Knowledge Platform!\");\n");
        sb.append("    }\n");
        sb.append("}\n");
        sb.append("```\n\n");
        
        sb.append("希望这些内容对大家有所帮助，欢迎在评论区留言讨论！\n");
        
        return sb.toString();
    }

    private String generateReplyContent() {
        String[] replies = {
            "感谢分享，受益匪浅！",
            "楼主说得太对了，深有同感。",
            "这个观点很有意思，我也去试试。",
            "请问有没有相关的参考资料推荐？",
            "支持一下，期待更多干货。",
            "这里面的代码逻辑好像有点问题，建议检查一下。",
            "Mark一下，回头细看。",
            "太强了，大佬带带我！"
        };
        return replies[new Random().nextInt(replies.length)];
    }
}
