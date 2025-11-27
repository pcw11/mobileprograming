package kr.ac.dongyang.mobileproject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.ac.dongyang.mobileproject.databinding.RegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private RegisterBinding binding;
    private boolean isIdChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = RegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 중복확인 버튼 클릭 리스너
        binding.btnCheckDuplicate.setOnClickListener(v -> {
            String userId = binding.etId.getText().toString();
            if (userId.isEmpty()) {
                Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            checkIdDuplicate(userId);
        });

        // 가입하기 버튼 클릭 리스너
        binding.btnRegister.setOnClickListener(v -> {
            if (!isIdChecked) {
                Toast.makeText(this, "아이디 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            String password = binding.etPassword.getText().toString();
            String passwordConfirm = binding.etPasswordConfirm.getText().toString();

            if (!password.equals(passwordConfirm)) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser();
        });

        // 아이디 입력 변경 시 중복확인 상태 초기화
        binding.etId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isIdChecked = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkIdDuplicate(final String userId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean isDuplicate = false;
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnector.getConnection();
                String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, userId);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    if (rs.getInt(1) > 0) {
                        isDuplicate = true;
                    }
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                // Close resources
            }

            boolean finalIsDuplicate = isDuplicate;
            handler.post(() -> {
                if (finalIsDuplicate) {
                    Toast.makeText(this, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show();
                    isIdChecked = false;
                } else {
                    Toast.makeText(this, "사용 가능한 아이디입니다.", Toast.LENGTH_SHORT).show();
                    isIdChecked = true;
                }
            });
        });
    }

    private void registerUser() {
        String userId = binding.etId.getText().toString();
        String password = binding.etPassword.getText().toString();
        String email = binding.etEmail.getText().toString();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = DatabaseConnector.getConnection();
                String sql = "INSERT INTO users (user_id, password, backup_email) VALUES (?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, userId);
                pstmt.setString(2, password);
                pstmt.setString(3, email);
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    success = true;
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                // Close resources
            }

            boolean finalSuccess = success;
            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                    finish(); // 회원가입 성공 시 로그인 화면으로 돌아감
                } else {
                    Toast.makeText(this, "회원가입에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
