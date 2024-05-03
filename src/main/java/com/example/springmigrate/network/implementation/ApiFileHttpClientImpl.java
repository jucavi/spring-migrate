package com.example.springmigrate.network.implementation;

import com.example.springmigrate.config.utils.RetrofitClient;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.network.IFileHttpClient;
import lombok.extern.log4j.Log4j2;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

@Component
@Log4j2
public class ApiFileHttpClientImpl {

    private final IFileHttpClient httpClient;

    public ApiFileHttpClientImpl(RetrofitClient retrofitClient) {
        httpClient = retrofitClient.getInstance()
                .create(IFileHttpClient.class);
    }

    public List<FileNodeDto> apiFindFiles() throws IOException {

        Call<List<FileNodeDto>> call = httpClient.finFiles();
        Response<List<FileNodeDto>> response = call.execute();

        //log.info("#apiFindTypes: {}", response.code());
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }

    public FileNodeDto apiCreateFile(FileNodeDto dto) throws IOException {

        Call<FileNodeDto> call = httpClient.createFile(dto);
        Response<FileNodeDto> response = call.execute();

        //log.info("#apiCreateFile({}): {}", response.code(), dto);
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }

    public FileNodeDto apiFindFileById(String id) throws IOException {

        Call<FileNodeDto> call = httpClient.findFileById(id);
        Response<FileNodeDto> response = call.execute();

        //log.info("#apiFindFileById({}): {}", response.code(), id);
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }

    public FileNodeDto apiUpdateFile(FileNodeDto dto, String id) throws IOException {

        Call<FileNodeDto> call = httpClient.updateFile(dto, id);
        Response<FileNodeDto> response = call.execute();

        //log.info("#apiUpdateDirectory({}): {}", response.code(), dto);
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }

    public void apiDeleteFileById(String id) throws IOException {

        Call<ResponseBody> call = httpClient.deleteFileById(id);
        Response<ResponseBody> response = call.execute();
        //log.info("#apiDeleteFileById({}): {}", response.code(), id);
    }
}