package com.example.springmigrate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DownloadNodeDto {

    private String authority;
    private ContentNodeDto content;
    private Integer defaultPort;
    //private DeserializedFieldsNodeDto deserializedFields;
    private String file;
    private String host;
    private String path;
    private Integer port;
    private String protocol;
    private String query;
    private String ref;
    private Integer serializedHashCode;
    private String userInfo;
}
