package com.esport.EsportTournament.dto;

import com.esport.EsportTournament.model.Banner;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BannerDTO {
    private int id;

    @JsonProperty("imageUrl")
    private String imageUrl;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("actionUrl")
    private String actionUrl;

    @JsonProperty("type")
    private String type;

    @JsonProperty("order")
    private int order;

    @JsonProperty("isActive")
    private boolean isActive;

    @JsonProperty("startDate")
    private LocalDateTime startDate;

    @JsonProperty("endDate")
    private LocalDateTime endDate;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}

