package com.example.springmigrate.network.implementation;

import com.example.springmigrate.config.utils.RetrofitClient;
import com.example.springmigrate.dto.FileTypeNodeDto;
import com.example.springmigrate.network.IFileTypeHttpClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

@Component
public class ApiFileTypeHttpClientImpl {

    private IFileTypeHttpClient httpClient;

    public ApiFileTypeHttpClientImpl(RetrofitClient retrofitClient) {
        httpClient = retrofitClient.getInstance()
                .create(IFileTypeHttpClient.class);
    }

    public List<FileTypeNodeDto> apiFindFiles() throws IOException {

        Call<List<FileTypeNodeDto>> call = httpClient.finFileTypes();
        Response<List<FileTypeNodeDto>> response = call.execute();

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }
}
