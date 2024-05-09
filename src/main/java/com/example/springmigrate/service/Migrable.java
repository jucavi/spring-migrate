package com.example.springmigrate.service;

import java.nio.file.Path;
import java.util.List;

public interface Migrable {

    void migrate(List<Path> paths);
}
