package com.fittrack.app.services;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class MongoApiClient {

    private static final String BASE_URL =
            "https://fittrack-backend-ivg0.onrender.com/";
    private static MongoPhotoApi api;

    public static synchronized MongoPhotoApi getApi() {
        if (api == null) {

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(90, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(
                            MoshiConverterFactory.create()
                    )
                    .build();

            api = retrofit.create(MongoPhotoApi.class);
        }

        return api;
    }
}