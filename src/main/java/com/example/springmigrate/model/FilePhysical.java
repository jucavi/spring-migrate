package com.example.springmigrate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePhysical {

    private String name;
    private String extension;
    private DirectoryPhysical parentDirectory;

}