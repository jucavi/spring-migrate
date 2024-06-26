package com.example.springmigrate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentDirectoryNodeDto {

    private Boolean active = true;
    private String exactName;
    private String fromDateTime;
    List<String> ids;
    private String name;
    private String parentDirectoryId;
    private String untilDateTime;
}
