package com.zoirs.learn_en_word;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LearnEnWordApplication {

	public static void main(String[] args) {
		SpringApplication.run(LearnEnWordApplication.class, args);
	}

}
