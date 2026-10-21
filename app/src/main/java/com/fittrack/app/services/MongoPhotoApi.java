
package com.fittrack.app.services;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface MongoPhotoApi {

    // Upload profile ya body-analysis photo
    @Multipart
    @POST("api/photos")
    Call<PhotoUploadResponse> uploadPhoto(
            @Header("Authorization") String authorization,
            @Part MultipartBody.Part photo,
            @Part("type") RequestBody type
    );

    // Download a private photo using its MongoDB photo ID
    @GET("api/photos/{id}")
    Call<ResponseBody> downloadPhoto(
            @Header("Authorization") String authorization,
            @Path("id") String photoId
    );

    // Delete a photo owned by the logged-in user
    @DELETE("api/photos/{id}")
    Call<ResponseBody> deletePhoto(
            @Header("Authorization") String authorization,
            @Path("id") String photoId
    );
}
