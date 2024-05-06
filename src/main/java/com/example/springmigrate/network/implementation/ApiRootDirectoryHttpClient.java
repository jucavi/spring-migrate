package com.example.springmigrate.network.implementation;

import com.example.springmigrate.config.utils.RetrofitClient;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.RootNodeDto;
import com.example.springmigrate.network.IRootDirectoryHttpClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;


@Component
@Log4j2
public class ApiRootDirectoryHttpClient {

    private final IRootDirectoryHttpClient httpClient;

    public ApiRootDirectoryHttpClient(RetrofitClient retrofitClient) {
        this.httpClient = retrofitClient.getInstance()
                .create(IRootDirectoryHttpClient.class);
    }

    public List<RootNodeDto> apiFindRootDirectories() throws IOException {

        Call<List<RootNodeDto>> call = httpClient.findRoots();
        Response<List<RootNodeDto>> response = call.execute();

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }

    public List<RootNodeDto> apiFindByDirectoryId(String directoryId) throws IOException {

        Call<List<RootNodeDto>> call = httpClient.findRootsByDirectoryId(directoryId);
        Response<List<RootNodeDto>> response = call.execute();

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }


}
