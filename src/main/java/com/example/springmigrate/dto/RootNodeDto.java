package com.example.springmigrate.dto;

import com.example.springmigrate.config.utils.AbstractEntityDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RootNodeDto extends AbstractEntityDto {

    private Boolean active = true;
    private String pathBase;
    private DirectoryNodeDto directory;
}