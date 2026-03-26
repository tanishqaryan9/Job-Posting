package com.Job.Posting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.Job.Posting")
@EnableScheduling
public class PostingApplication {

	public static void main(String[] args) {
		SpringApplication.run(PostingApplication.class, args);
	}

}
