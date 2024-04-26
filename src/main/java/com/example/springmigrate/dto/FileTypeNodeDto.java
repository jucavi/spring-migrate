package com.example.springmigrate.dto;


import com.example.springmigrate.config.utils.AbstractEntityDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileTypeNodeDto extends AbstractEntityDto {

    String id;
    String name;
    String extension;
    String mimeType;
}
