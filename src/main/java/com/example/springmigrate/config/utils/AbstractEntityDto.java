package com.example.springmigrate.config.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;


/**
 * Abstract dto for entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractEntityDto {

    private String id;
    private Integer versionLock;
    private String insertDate;
    private String modificationDate;
}
