package kr.ac.dongyang.mobileproject;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import kr.ac.dongyang.mobileproject.databinding.FindpwBinding;

public class FindPasswordActivity extends AppCompatActivity {

    private FindpwBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FindpwBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // (추가) 이곳에 '인증' 버튼, '변경하기' 버튼 등의
        // 클릭 리스너 로직을 구현하면 됩니다.
        // 예: binding.btnVerifyEmail.setOnClickListener(...);
    }
}