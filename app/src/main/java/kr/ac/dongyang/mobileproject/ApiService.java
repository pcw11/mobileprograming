package kr.ac.dongyang.mobileproject;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("/upload")
    Call<String> uploadImage(@Part MultipartBody.Part image);
}
