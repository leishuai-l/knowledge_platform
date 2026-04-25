package com.zhixiang.knowledge_platform.config;

import com.zhixiang.knowledge_platform.entity.ForumCategory;
import com.zhixiang.knowledge_platform.repository.ForumCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ForumDataInitializer {

    private final ForumCategoryRepository categoryRepository;

    @Bean
    public CommandLineRunner initForumData() {
        return args -> {
            log.info("Initializing forum data...");
            
            initCategory("学习交流", "分享学习心得，交流学习方法", "Reading", 1);
            initCategory("资源分享", "优质学习资源、电子书、课件分享", "Files", 2);
            initCategory("技术问答", "编程、科研、学术问题求助与解答", "QuestionFilled", 3);
            initCategory("闲聊灌水", "课余生活、兴趣爱好、日常吐槽", "Coffee", 4);
            
            log.info("Forum data initialization completed.");
        };
    }

    private void initCategory(String name, String description, String icon, Integer sortOrder) {
        ForumCategory category = categoryRepository.findByName(name)
                .orElseGet(() -> {
                    ForumCategory c = new ForumCategory();
                    c.setName(name);
                    return c;
                });
        
        category.setDescription(description);
        category.setIcon(icon);
        category.setSortOrder(sortOrder);
        category.setStatus(true); // Ensure status is true
        
        categoryRepository.save(category);
    }
}
