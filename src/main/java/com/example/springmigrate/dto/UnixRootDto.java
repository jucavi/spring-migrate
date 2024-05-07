package com.example.springmigrate.dto;

import com.example.springmigrate.model.DirectoryPhysical;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnixRootDto {

    DirectoryPhysical rootDirectory;
    DirectoryNodeDto rootNode;
}
