package com.example.springmigrate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentNodeDto {

    private String id;
    private Boolean active;
    private String exactName;
    private String fromDateTime;
    List<String> ids;
    private String name;
    private String parentDirectoryId;
    private String untilDateTime;
}
