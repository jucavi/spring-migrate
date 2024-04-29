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
public class DirectoryNodeDto extends AbstractEntityDto {

    private String name;
    private String parentDirectoryId;
    private String pathBase;
    private Boolean active = true;
}
