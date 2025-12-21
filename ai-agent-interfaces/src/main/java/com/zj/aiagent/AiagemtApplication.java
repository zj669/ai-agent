package com.zj.aiagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan("com.zj.aiagent.infrastructure.persistence.mapper")
public class AiagemtApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiagemtApplication.class, args);
	}

}