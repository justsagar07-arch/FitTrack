package com.fittrack.app.services;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MongoPhotoUploadService {

    private static final int MAX_PHOTO_SIZE = 5 * 1024 * 1024;

    public interface UploadCallback {
        void onSuccess(String photoId);
        void onFailure(String error);
    }

    public static void uploadPhoto(
            Context context,
            Uri photoUri,
            String type,
            UploadCallback callback
    ) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            callback.onFailure("Please log in first.");
            return;
        }

        if (!"profile".equals(type) && !"body-analysis".equals(type)) {
            callback.onFailure("Unsupported photo type.");
            return;
        }

        try {
            byte[] photoBytes = readPhoto(context, photoUri);

            if (photoBytes.length == 0) {
                callback.onFailure("Selected photo is empty.");
                return;
            }

            if (photoBytes.length > MAX_PHOTO_SIZE) {
                callback.onFailure("Photo must be 5 MB or smaller.");
                return;
            }

            String mimeType = getPhotoMimeType(context, photoUri);

            if (mimeType == null) {
                callback.onFailure(
                        "Unsupported photo format. Use JPG, PNG or WebP."
                );
                return;
            }

            String fileName = "fittrack-photo." + getExtension(mimeType);

            RequestBody imageBody = RequestBody.create(
                    MediaType.parse(mimeType),
                    photoBytes
            );

            MultipartBody.Part photoPart =
                    MultipartBody.Part.createFormData(
                            "photo",
                            fileName,
                            imageBody
                    );

            RequestBody typeBody = RequestBody.create(
                    MediaType.parse("text/plain"),
                    type
            );

            user.getIdToken(false)
                    .addOnSuccessListener(result -> {
                        String token = result.getToken();

                        if (token == null || token.isEmpty()) {
                            callback.onFailure(
                                    "Could not get login token."
                            );
                            return;
                        }

                        MongoApiClient.getApi()
                                .uploadPhoto(
                                        "Bearer " + token,
                                        photoPart,
                                        typeBody
                                )
                                .enqueue(new Callback<PhotoUploadResponse>() {

                                    @Override
                                    public void onResponse(
                                            Call<PhotoUploadResponse> call,
                                            Response<PhotoUploadResponse> response
                                    ) {
                                        if (response.isSuccessful()
                                                && response.body() != null
                                                && response.body().getPhotoId() != null) {

                                            callback.onSuccess(
                                                    response.body().getPhotoId()
                                            );
                                        } else {
                                            callback.onFailure(
                                                    "Upload failed. HTTP "
                                                            + response.code()
                                            );
                                        }
                                    }

                                    @Override
                                    public void onFailure(
                                            Call<PhotoUploadResponse> call,
                                            Throwable error
                                    ) {
                                        callback.onFailure(
                                                error.getMessage() == null
                                                        ? "Network error"
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

        } catch (Exception error) {
            callback.onFailure(
                    "Could not read photo: " + error.getMessage()
            );
        }
    }

    private static String getPhotoMimeType(
            Context context,
            Uri uri
    ) {
        String mimeType = context.getContentResolver().getType(uri);

        // CameraX saves the captured photo as a local .jpg file.
        if (mimeType == null && "file".equals(uri.getScheme())) {
            String path = uri.getPath();

            if (path != null) {
                String extension = MimeTypeMap.getFileExtensionFromUrl(path);

                if (extension != null) {
                    mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(
                                    extension.toLowerCase(Locale.ROOT)
                            );
                }

                // Our CameraX output filename ends in .jpg.
                if (mimeType == null
                        && path.toLowerCase(Locale.ROOT).endsWith(".jpg")) {
                    mimeType = "image/jpeg";
                }
            }
        }

        if ("image/jpg".equalsIgnoreCase(mimeType)) {
            mimeType = "image/jpeg";
        }

        if (mimeType == null) {
            return null;
        }

        mimeType = mimeType.toLowerCase(Locale.ROOT);

        if (mimeType.equals("image/jpeg")
                || mimeType.equals("image/png")
                || mimeType.equals("image/webp")) {
            return mimeType;
        }

        return null;
    }

    private static String getExtension(String mimeType) {
        if ("image/png".equals(mimeType)) {
            return "png";
        }

        if ("image/webp".equals(mimeType)) {
            return "webp";
        }

        return "jpg";
    }

    private static byte[] readPhoto(
            Context context,
            Uri uri
    ) throws IOException {

        try (InputStream input =
                     context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            if (input == null) {
                // Fallback for a local file URI.
                if ("file".equals(uri.getScheme())) {
                    String path = uri.getPath();

                    if (path != null) {
                        try (InputStream fileInput =
                                     new java.io.FileInputStream(
                                             new File(path)
                                     )) {
                            byte[] buffer = new byte[8192];
                            int count;

                            while ((count = fileInput.read(buffer)) != -1) {
                                output.write(buffer, 0, count);
                            }

                            return output.toByteArray();
                        }
                    }
                }

                throw new IOException("Cannot open selected photo.");
            }

            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            return output.toByteArray();
        }
    }
}
