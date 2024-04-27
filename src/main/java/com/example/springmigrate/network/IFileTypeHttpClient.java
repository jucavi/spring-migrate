package com.example.springmigrate.network;


import com.example.springmigrate.dto.FileTypeNodeDto;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface IFileTypeHttpClient {

    @GET("/types")
    Call<List<FileTypeNodeDto>> finFileTypes();
}
