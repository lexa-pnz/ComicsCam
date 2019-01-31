package com.example.alex.comicscamtest;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;

public interface ApiConfig{

        @Multipart
        @POST("retrofit/images/upload_image.php")
        Call<ServerResponse> upload(
                @PartMap Map<String, RequestBody> map
        );
}
