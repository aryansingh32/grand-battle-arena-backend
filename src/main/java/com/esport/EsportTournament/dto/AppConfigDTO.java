package com.esport.EsportTournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfigDTO {
    private String key;
    private String value;
    private String type;
    private String description;
}
