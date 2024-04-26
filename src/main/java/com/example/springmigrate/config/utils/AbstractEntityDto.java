package com.example.springmigrate.config.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * Abstract dto for entities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractEntityDto {

    private String id;
    private Integer versionLock;
    private String insertDate;
    private String modificationDate;
}
