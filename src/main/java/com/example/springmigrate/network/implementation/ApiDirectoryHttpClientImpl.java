package com.example.springmigrate.network.implementation;

import com.example.springmigrate.config.utils.RetrofitClient;
import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.PaginatedListDto;
import com.example.springmigrate.network.IDirectoryHttpClient;
import lombok.extern.log4j.Log4j2;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

@Component
@Log4j2
public class ApiDirectoryHttpClientImpl {

    private final IDirectoryHttpClient httpClient;

    public ApiDirectoryHttpClientImpl(RetrofitClient retrofitClient) {
        httpClient = retrofitClient.getInstance()
                .create(IDirectoryHttpClient.class);
    }

    public List<DirectoryNodeDto> apiFindDirectories() throws IOException {

        Call<List<DirectoryNodeDto>> call = httpClient.finDirectories();
        Response<List<DirectoryNodeDto>> response = call.execute();

        log.info("#apiFindDirectories: {}", response.code());
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }

    public List<DirectoryNodeDto> apiCreateDirectoryHierarchicallyLogical(List<DirectoryNodeDto> dtos) throws IOException {

        Call<List<DirectoryNodeDto>> call = httpClient.createDirectoryHierarchicallyLogical(dtos);
        Response<List<DirectoryNodeDto>> response = call.execute();

        log.info("#apiCreateDirectoryHierarchicallyLogical: {}", response.code());
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }


    public DirectoryNodeDto apiFindDirectoryById(String id) throws IOException {

        Call<DirectoryNodeDto> call = httpClient.findDirectoryById(id);
        Response<DirectoryNodeDto> response = call.execute();

        log.info("#apiFindDirectoryById: {}", response.code());
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }


    public DirectoryNodeDto apiUpdateDirectory(DirectoryNodeDto dto) throws IOException {

        Call<DirectoryNodeDto> call = httpClient.updateDirectory(dto.getId(), dto);
        Response<DirectoryNodeDto> response = call.execute();

        log.info("#apiUpdateDirectory: {}", response.code());
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }


    public List<DirectoryNodeDto> apiSearchAllDirectoriesByFilter(DirectoryFilterNodeDto dtos) throws IOException {

        Call<PaginatedListDto<DirectoryNodeDto>> call = httpClient.searchAllDirectoriesByFilter(dtos);
        Response<PaginatedListDto<DirectoryNodeDto>> response = call.execute();

        log.info("#apiSearchAllDirectoriesByFilter: {}", response.code());
        if (!response.isSuccessful() || response.body() == null) {
            return null;
        }

        return response.body().getResults();
    }

    public DirectoryNodeDto apiSearchDirectoryByFilter(DirectoryFilterNodeDto dto) throws IOException {

        Call<DirectoryNodeDto> call = httpClient.searchDirectoryByFilter(dto);
        Response<DirectoryNodeDto> response = call.execute();

        log.info("#apiSearchDirectoryByFilter: {}", response.code());
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }


    public void apiDeleteDirectoryById(String id) throws IOException {

        Call<ResponseBody> call = httpClient.deleteDirectoryById(id);
        Response<ResponseBody> response = call.execute();
        log.info("#apiDeleteDirectoryById: {}", response.code());
    }

    public void apiDeleteDirectoryHardById(String id) throws IOException {

        Call<ResponseBody> call = httpClient.deleteDirectoryHardById(id);
        Response<ResponseBody> response = call.execute();

        log.info("#apiDeleteDirectoryHardById: {}", response.code());
    }
}
