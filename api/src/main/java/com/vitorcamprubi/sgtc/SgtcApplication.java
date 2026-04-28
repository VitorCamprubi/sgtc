package com.vitorcamprubi.sgtc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
<<<<<<< HEAD
import org.springframework.scheduling.annotation.EnableAsync;
=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
<<<<<<< HEAD
@EnableAsync
=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
public class SgtcApplication {

	public static void main(String[] args) {
		SpringApplication.run(SgtcApplication.class, args);
	}

}
