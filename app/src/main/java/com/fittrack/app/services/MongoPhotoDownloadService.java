
package com.fittrack.app.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MongoPhotoDownloadService {

    public interface DownloadCallback {
        void onSuccess(Bitmap bitmap);
        void onFailure(String error);
    }

    public static void downloadPhoto(
            Context context,
            String photoId,
            DownloadCallback callback
    ) {
        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onFailure("Please log in first.");
            return;
        }

        if (photoId == null || photoId.trim().isEmpty()) {
            callback.onFailure("Photo ID not found.");
            return;
        }

        user.getIdToken(false)
                .addOnSuccessListener(tokenResult -> {
                    String token = tokenResult.getToken();

                    if (token == null || token.isEmpty()) {
                        callback.onFailure(
                                "Could not get login token."
                        );
                        return;
                    }

                    MongoApiClient.getApi()
                            .downloadPhoto(
                                    "Bearer " + token,
                                    photoId
                            )
                            .enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(
                                        Call<ResponseBody> call,
                                        Response<ResponseBody> response
                                ) {
                                    if (!response.isSuccessful()
                                            || response.body() == null) {
                                        callback.onFailure(
                                                "Photo download failed. HTTP "
                                                        + response.code()
                                        );
                                        return;
                                    }

                                    try (ResponseBody body =
                                                 response.body()) {
                                        Bitmap bitmap =
                                                BitmapFactory.decodeStream(
                                                        body.byteStream()
                                                );

                                        if (bitmap == null) {
                                            callback.onFailure(
                                                    "Could not decode photo."
                                            );
                                            return;
                                        }

                                        callback.onSuccess(bitmap);

                                    } catch (Exception error) {
                                        callback.onFailure(
                                                "Could not read photo: "
                                                        + error.getMessage()
                                        );
                                    }
                                }

                                @Override
                                public void onFailure(
                                        Call<ResponseBody> call,
                                        Throwable error
                                ) {
                                    callback.onFailure(
                                            error.getMessage() == null
                                                    ? "Network error."
                                                    : error.getMessage()
                                    );
                                }
                            });
                })
                .addOnFailureListener(error ->
                        callback.onFailure(
                                "Could not verify login: "
                                        + error.getMessage()
                        )
                );
    }
}
