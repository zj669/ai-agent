package com.zj.aiagemt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
		"com.zj.aiagemt.mapper.entity",
})
public class AiagemtApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(AiagemtApplication.class, args);
	}

}