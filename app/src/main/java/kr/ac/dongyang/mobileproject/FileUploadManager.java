package kr.ac.dongyang.mobileproject;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class FileUploadManager {

    private final ApiService apiService;
    private final Context context;

    // 콜백 인터페이스 정의
    public interface FileUploadCallback {
        void onUploadSuccess(String imageUrl);
        void onUploadFailure(String message);
    }

    public FileUploadManager(Context context) {
        this.context = context;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://" + BuildConfig.SERVER_IP + ":6006/") // BuildConfig에서 IP 주소 사용
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    public void uploadImage(Uri imageUri, @NonNull FileUploadCallback callback) {
        File tempFile = createTempFileFromUri(imageUri);
        if (tempFile == null) {
            callback.onUploadFailure("이미지 파일을 준비하는데 실패했습니다.");
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse(context.getContentResolver().getType(imageUri)), tempFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);

        Call<String> call = apiService.uploadImage(body);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                tempFile.delete(); // 임시 파일 삭제
                if (response.isSuccessful()) {
                    callback.onUploadSuccess(response.body());
                } else {
                    String errorMessage = "Upload failed: " + response.code() + " " + response.message();
                    try {
                        Log.e("UPLOAD_ERROR", "Error Body: " + (response.errorBody() != null ? response.errorBody().string() : "null"));
                    } catch (Exception e) {
                        Log.e("UPLOAD_ERROR", "Error reading error body", e);
                    }
                    callback.onUploadFailure(errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                tempFile.delete(); // 임시 파일 삭제
                Log.e("UPLOAD_FAILURE", "Upload error: " + t.getMessage(), t);
                callback.onUploadFailure("Upload error: " + t.getMessage());
            }
        });
    }

    private File createTempFileFromUri(Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) return null;

        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String fileName = cursor.getString(nameIndex);
        cursor.close();

        File tempFile = null;
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            tempFile = new File(context.getCacheDir(), fileName);
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            Log.e("FileCreation", "Error creating temp file", e);
            return null;
        }
        return tempFile;
    }

    public interface ApiService {
        @Multipart
        @POST("/upload")
        Call<String> uploadImage(@Part MultipartBody.Part image);
    }
}
