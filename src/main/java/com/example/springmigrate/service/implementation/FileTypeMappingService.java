package com.example.springmigrate.service.implementation;

import com.example.springmigrate.service.IFileTypeLogicalService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;


@Component
public class FileTypeMappingService {

    private final Map<String, String> types;

    public FileTypeMappingService(IFileTypeLogicalService service) throws IOException {
        this.types = service.findAllFileTypes();
    }

    public String getFileExtension(String mimeType) {
        return types.get(mimeType);
    }
}
