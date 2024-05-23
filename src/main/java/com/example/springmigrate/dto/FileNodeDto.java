package com.example.springmigrate.dto;


import com.example.springmigrate.config.utils.AbstractEntityDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileNodeDto extends AbstractEntityDto {

    private Boolean active = true;
    private String fileData;
    private String mimeType;
    private String name;
    private String parentDirectoryId;
    private String pathBase;
    private Integer version;
    List<FileExportDto> fileExports;

}
