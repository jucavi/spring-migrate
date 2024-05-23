package com.example.springmigrate.dto;

import com.example.springmigrate.config.utils.AbstractEntityDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectoryFilterNodeDto extends AbstractEntityDto {

    private ContentDirectoryNodeDto content;
    private Integer page;
    private Integer size;
}
