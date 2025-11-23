// package com.esport.EsportTournament.config;

// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
// import org.springframework.boot.context.properties.ConfigurationProperties;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Primary;

// import javax.sql.DataSource;
// import java.net.URI;
// import java.net.URISyntaxException;

// /**
//  * Configuration for Railway deployment
//  * Parses DATABASE_URL from Railway environment variable
//  */
// @Slf4j
// @Configuration
// public class DatabaseConfig {

//     @Value("${DATABASE_URL:}")
//     private String databaseUrl;

//     @Bean
//     @Primary
//     @ConfigurationProperties("spring.datasource")
//     public DataSourceProperties dataSourceProperties() {
//         return new DataSourceProperties();
//     }

//     @Bean
//     @Primary
//     public DataSource dataSource(DataSourceProperties properties) {
//         // If DATABASE_URL is provided (Railway), parse it
//         if (databaseUrl != null && !databaseUrl.isEmpty() && databaseUrl.startsWith("postgres://")) {
//             try {
//                 log.info("Parsing DATABASE_URL from Railway");
//                 return parseRailwayDatabaseUrl(databaseUrl);
//             } catch (URISyntaxException e) {
//                 log.error("Failed to parse DATABASE_URL, falling back to properties", e);
//             }
//         }
        
//         // Otherwise, use Spring Boot auto-configuration with individual properties
//         log.info("Using standard datasource configuration");
//         return properties.initializeDataSourceBuilder().build();
//     }

//     private DataSource parseRailwayDatabaseUrl(String databaseUrl) throws URISyntaxException {
//         URI dbUri = new URI(databaseUrl);
        
//         String username = dbUri.getUserInfo().split(":")[0];
//         String password = dbUri.getUserInfo().split(":")[1];
//         String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
        
//         // Handle query parameters (e.g., ?sslmode=require)
//         if (dbUri.getQuery() != null) {
//             dbUrl += "?" + dbUri.getQuery();
//         }

//         log.info("Database URL parsed: jdbc:postgresql://{}:{}{}", dbUri.getHost(), dbUri.getPort(), dbUri.getPath());

//         return org.springframework.boot.jdbc.DataSourceBuilder.create()
//                 .url(dbUrl)
//                 .username(username)
//                 .password(password)
//                 .driverClassName("org.postgresql.Driver")
//                 .build();
//     }
// }

