package com.example.springmigrate.network;


import com.example.springmigrate.dto.RootNodeDto;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

public interface IRootDirectoryHttpClient {

    @GET("/roots")
    Call<List<RootNodeDto>> findRoots();

    @GET("/filterBy/{directoryId}")
    Call<List<RootNodeDto>> findRootsByDirectoryId(@Path("directoryId") String directoryId);

    @DELETE("/directory/{directoryId}")
    Call<ResponseBody> deleteByDirectoryId(@Path("directoryId") String directoryId);
}
