package com.esport.EsportTournament;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EsportTournamentApplication {

	public static void main(String[] args) {
		SpringApplication.run(EsportTournamentApplication.class, args);
	}

}
