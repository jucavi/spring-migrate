package com.example.springmigrate.network;

import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface IDirectoryHttpClient {

    @GET("/directories")
    Call<List<DirectoryNodeDto>> finDirectories();

    @POST("/directories")
    Call<List<DirectoryNodeDto>> createDirectoryHierarchicallyPhysical(@Body DirectoryNodeDto dto);

    @GET("/directories/{id}")
    Call<DirectoryNodeDto> findDirectoryById(@Path("id") String id);

    @PUT("/directories/{id}")
    Call<DirectoryNodeDto> updateDirectory(@Path("id") String id, @Body DirectoryNodeDto dto);

    @POST("/directories/searchAll")
    Call<List<DirectoryNodeDto>> searchAllDirectoriesByFilter(@Body List<DirectoryFilterNodeDto> dtos);

    @POST("/directories/searchOne")
    Call<List<DirectoryNodeDto>> searchDirectoryByFilter(@Body List<DirectoryFilterNodeDto> dtos);

    @DELETE("/directories/{id}")
    Call<Integer> deleteDirectoryById(@Path("id") String id);
}
