package com.unifapi.academico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AcademicoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcademicoBackendApplication.class, args);
	}

}
