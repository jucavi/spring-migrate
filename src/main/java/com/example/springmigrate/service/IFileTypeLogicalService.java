package com.example.springmigrate.service;


import java.io.IOException;
import java.util.Map;

public interface IFileTypeLogicalService {

    Map<String, String> findAllFileTypes() throws IOException;
}
