package com.example.springmigrate.dto;


import com.example.springmigrate.config.utils.AbstractEntityDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileNodeDto extends AbstractEntityDto {

    private String name;
    private String parentDirectoryId;
    private String pathBase;
    private String mimeType;
    private String fileData;
    private Integer version;
    private Boolean active = true;

    List<FileExportDto> fileExports;
}
