package com.example.springmigrate.network.implementation;

import com.example.springmigrate.config.utils.RetrofitClient;
import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.network.IDirectoryHttpClient;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

@Component
public class ApiDirectoryHttpClientImpl {

    private final IDirectoryHttpClient httpClient;

    public ApiDirectoryHttpClientImpl() {
        httpClient = RetrofitClient.getInstance()
                .create(IDirectoryHttpClient.class);
    }

    public List<DirectoryNodeDto> apiFindDirectories() throws IOException {

        Call<List<DirectoryNodeDto>> call = httpClient.finDirectories();
        Response<List<DirectoryNodeDto>> response = call.execute();

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }

    public List<DirectoryNodeDto> apiCreateDirectoryHierarchicallyPhysical(DirectoryNodeDto dto) throws IOException {

        Call<List<DirectoryNodeDto>> call = httpClient.createDirectoryHierarchicallyPhysical(dto);
        Response<List<DirectoryNodeDto>> response = call.execute();

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }


    public DirectoryNodeDto apiFindDirectoryById(String id) throws IOException {

        Call<DirectoryNodeDto> call = httpClient.findDirectoryById(id);
        Response<DirectoryNodeDto> response = call.execute();

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }


    public DirectoryNodeDto apiUpdateDirectory(String id, DirectoryNodeDto dto) throws IOException {

        Call<DirectoryNodeDto> call = httpClient.updateDirectory(id, dto);
        Response<DirectoryNodeDto> response = call.execute();

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }


    public List<DirectoryNodeDto> apiSearchAllDirectoriesByFilter(List<DirectoryFilterNodeDto> dtos) throws IOException {

        Call<List<DirectoryNodeDto>> call = httpClient.searchAllDirectoriesByFilter(dtos);
        Response<List<DirectoryNodeDto>> response = call.execute();

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }

    public List<DirectoryNodeDto> apiSearchDirectoryByFilter(List<DirectoryFilterNodeDto> dtos) throws IOException {

        Call<List<DirectoryNodeDto>> call = httpClient.searchDirectoryByFilter(dtos);
        Response<List<DirectoryNodeDto>> response = call.execute();

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }


    public void apiDeleteDirectoryById(String id) throws IOException {

        Call<Integer> call = httpClient.deleteDirectoryById(id);
        call.execute();
    }
}
