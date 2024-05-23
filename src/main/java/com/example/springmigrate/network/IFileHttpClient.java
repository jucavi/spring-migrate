package com.example.springmigrate.network;


import com.example.springmigrate.dto.FileFilterDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.dto.PaginatedListDto;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface IFileHttpClient {

    @GET("/files")
    Call<List<FileNodeDto>> findFiles();

    @POST("/files")
    Call<FileNodeDto> createFile(@Body FileNodeDto dto);

    @GET("/files/{id}")
    Call<FileNodeDto> findFileById(@Path("id") String id);

    @PUT("/files/{id}")
    Call<FileNodeDto> updateFile(@Body FileNodeDto dto, @Path("id") String id);

    @DELETE("/files/{id}")
    Call<ResponseBody> deleteFileById(@Path("id") String id);

    @POST("/files/searchAll")
    Call<PaginatedListDto<FileNodeDto>> findFilesByFilter(@Body FileFilterDto filter);
}
