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
public class ContentFileNodeDto {

    private Boolean active = true;
    private String exactName;
    private Integer fileTypeId;
    private String fromDateTime;
    List<String> ids;
    private Boolean includeData;
    private String name;
    private String parentDirectoryId;
    private String untilDateTime;
    private Integer version = 0;
}
