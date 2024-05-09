package com.example.springmigrate.network;


import com.example.springmigrate.dto.RootNodeDto;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface IRootDirectoryHttpClient {

    @GET("/roots")
    Call<List<RootNodeDto>> findRoots();

    @GET("/roots/filterBy/{directoryId}")
    Call<List<RootNodeDto>> findRootsByDirectoryId(@Path("directoryId") String directoryId);

    @DELETE("/roots/directory/{directoryId}")
    Call<ResponseBody> deleteByDirectoryId(@Path("directoryId") String directoryId);

    @POST("/roots")
    Call<RootNodeDto> createRoot(@Body RootNodeDto rootNode);

    @DELETE("/roots/truncate")
    Call<ResponseBody> truncate();
}
