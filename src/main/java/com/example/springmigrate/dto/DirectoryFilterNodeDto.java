package com.example.springmigrate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectoryFilterNodeDto {

    private ContentNodeDto content;
    private Integer page;
    private Integer size;
}
