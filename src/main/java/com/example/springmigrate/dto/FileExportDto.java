package com.example.springmigrate.dto;


import com.example.springmigrate.config.utils.AbstractEntityDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileExportDto extends AbstractEntityDto {

    private Boolean active = true;
    private String expirationDate;
    private String file;
    private DownloadNodeDto download;
}
