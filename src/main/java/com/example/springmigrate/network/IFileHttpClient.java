package com.example.springmigrate.network;


import com.example.springmigrate.dto.FileNodeDto;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface IFileHttpClient {

    @GET("/files")
    Call<List<FileNodeDto>> finFiles();

    @POST("/files")
    Call<FileNodeDto> createFile(@Body FileNodeDto dto);

    @GET("/files/{id}")
    Call<FileNodeDto> findFileById(@Path("id") String id);

    @PUT("/files/{id}")
    Call<FileNodeDto> updateFile(@Path("id") String id, @Body FileNodeDto dto);

    @DELETE("/files/{id}")
    Call<Integer> deleteFileById(@Path("id") String id);

}
