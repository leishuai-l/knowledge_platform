package com.zhixiang.knowledge_platform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration;
import org.springframework.ai.autoconfigure.vectorstore.qdrant.QdrantVectorStoreAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 知享校园知识库共享平台 - 启动类
 * 
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 * @since 2024
 */
@SpringBootApplication(exclude = {QdrantVectorStoreAutoConfiguration.class, OllamaAutoConfiguration.class})
@EnableJpaRepositories(basePackages = "com.zhixiang.knowledge_platform.repository")
@EnableAsync
@Slf4j
public class KnowledgePlatformApplication {

	public static void main(String[] args) {
		// 设置系统属性
		System.setProperty("spring.application.name", "知享校园知识库共享平台");
		
		ConfigurableApplicationContext context = SpringApplication.run(KnowledgePlatformApplication.class, args);
		
		// 获取环境信息
		Environment env = context.getEnvironment();
		printStartupInfo(env);
	}

	/**
	 * 打印启动信息
	 */
	private static void printStartupInfo(Environment env) {
		try {
			String serverPort = env.getProperty("server.port", "8080");
			String contextPath = env.getProperty("server.servlet.context-path", "");

			log.info("\n" +
				"  ______  _     _ _      _                 \n" +
				" |___  / | |   (_) |    (_)                \n" +
				"    / /  | |__  _ | |_   _  __ _ _ __   __ _ \n" +
				"   / /   | '_ \\| || __| | |/ _` | '_ \\ / _` |\n" +
				"  / /__  | | | | || |_  | | (_| | | | | (_| |\n" +
				" /_____|_|_| |_|_| \\__| |_|\\__,_|_| |_|\\__, |\n" +
				" ==================================     __/ |\n" +
				"                                       |___/ \n" +
				"\n🚀 后端服务: http://localhost:{}{}\n" +
				"📚 接口文档: http://localhost:{}{}/swagger-ui.html\n",
				serverPort, contextPath,
				serverPort, contextPath);

		} catch (Exception e) {
			log.error("启动信息显示异常", e);
		}
	}

	/**
	 * 应用运行后回调
	 */
	@Component
	@Slf4j
	static class ApplicationStartupRunner implements ApplicationRunner {
		@Override
		public void run(ApplicationArguments args) {
			log.info("✅ 知享平台启动完成");
		}
	}
}
