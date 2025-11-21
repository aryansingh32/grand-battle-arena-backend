package com.esport.EsportTournament.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class password {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        System.out.println(encoder.encode("Admin@Qr"));
    }

}
