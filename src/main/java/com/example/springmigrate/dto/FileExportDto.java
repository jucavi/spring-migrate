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
public class FileExportDto extends AbstractEntityDto {

    private String expirationDate;
    private String file;
    private DownloadNodeDto download;
}
