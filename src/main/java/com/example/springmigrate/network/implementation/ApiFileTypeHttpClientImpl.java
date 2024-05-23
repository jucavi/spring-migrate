package com.example.springmigrate.network.implementation;

import com.example.springmigrate.config.utils.RetrofitClient;
import com.example.springmigrate.dto.FileTypeNodeDto;
import com.example.springmigrate.network.IFileTypeHttpClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

@Component
@Log4j2
public class ApiFileTypeHttpClientImpl {

    private final IFileTypeHttpClient httpClient;

    public ApiFileTypeHttpClientImpl(RetrofitClient retrofitClient) throws ConnectException {
        httpClient = retrofitClient.getInstance()
                .create(IFileTypeHttpClient.class);
    }

    public List<FileTypeNodeDto> apiFindTypes() throws IOException {

        Call<List<FileTypeNodeDto>> call = httpClient.finFileTypes();
        Response<List<FileTypeNodeDto>> response = call.execute();

        log.info("#apiFindTypes: {}", response.code());
        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }
}
