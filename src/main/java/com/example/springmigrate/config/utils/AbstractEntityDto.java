package com.example.springmigrate.config.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


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
