package com.example.springmigrate.network;

import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.PaginatedListDto;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface IDirectoryHttpClient {

    @GET("/directories")
    Call<List<DirectoryNodeDto>> finDirectories();

    @POST("/directories/create-all/logical")
    Call<List<DirectoryNodeDto>> createDirectoryHierarchicallyLogical(@Body List<DirectoryNodeDto> dtos);

    @GET("/directories/{id}")
    Call<DirectoryNodeDto> findDirectoryById(@Path("id") String id);

    @PUT("/directories/{id}")
    Call<DirectoryNodeDto> updateDirectory(@Path("id") String id, @Body DirectoryNodeDto dto);

    @POST("/directories/searchAll")
    Call<PaginatedListDto<DirectoryNodeDto>> searchAllDirectoriesByFilter(@Body DirectoryFilterNodeDto dto);

    @POST("/directories/searchOne")
    Call<DirectoryNodeDto> searchDirectoryByFilter(@Body DirectoryFilterNodeDto dto);

    @DELETE("/directories/{id}")
    Call<ResponseBody> deleteDirectoryById(@Path("id") String id);

    @DELETE("/directories/{id}/purge")
    Call<ResponseBody> deleteDirectoryHardById(@Path("id") String id);
}
