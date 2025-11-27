package kr.ac.dongyang.mobileproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.ac.dongyang.mobileproject.databinding.LoginBinding;

public class LoginActivity extends AppCompatActivity {

    private LoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = LoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvFindPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FindPasswordActivity.class);
            startActivity(intent);
        });

        binding.tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        TextWatcher loginTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                chkEmpty();
            }
        };

        binding.etId.addTextChangedListener(loginTextWatcher);
        binding.etPassword.addTextChangedListener(loginTextWatcher);

        binding.btnLogin.setOnClickListener(v -> {
            String userId = binding.etId.getText().toString();
            String password  = binding.etPassword.getText().toString();

            login(userId, password);
        });

        chkEmpty(); // 초기 버튼 상태 설정
    }

    private void login(final String userId, final String password) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String resultMessage = null;
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                conn = DatabaseConnector.getConnection();
                String sql = "SELECT * FROM users WHERE user_id = ? AND password = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, userId);
                pstmt.setString(2, password);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    resultMessage = "SUCCESS";
                } else {
                    resultMessage = "아이디 또는 비밀번호가 일치하지 않습니다.";
                }
            } catch (ClassNotFoundException e) {
                resultMessage = "데이터베이스 드라이버를 찾을 수 없습니다.";
                Log.e("LoginActivity", "JDBC Driver not found", e);
            } catch (SQLException e) {
                resultMessage = "데이터베이스 연결에 실패했습니다. 네트워크 상태를 확인해주세요.";
                Log.e("LoginActivity", "SQL Exception", e);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    Log.e("LoginActivity", "Error closing resources", e);
                }
            }

            String finalResultMessage = resultMessage;
            handler.post(() -> {
                if ("SUCCESS".equals(finalResultMessage)) {
                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // 로그인 성공 시 현재 액티비티 종료
                } else {
                    Toast.makeText(LoginActivity.this, finalResultMessage, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void chkEmpty(){
        boolean isIdEmpty = binding.etId.getText().toString().isEmpty();
        boolean isPwEmpty = binding.etPassword.getText().toString().isEmpty();

        if (isIdEmpty || isPwEmpty) {
            binding.btnLogin.setBackgroundResource(R.drawable.login_n_btn);
            binding.btnLogin.setTextColor(ContextCompat.getColor(this, R.color.gray));
            binding.btnLogin.setEnabled(false);
        } else {
            binding.btnLogin.setBackgroundResource(R.drawable.login_y_btn);
            binding.btnLogin.setTextColor(ContextCompat.getColor(this, R.color.white));
            binding.btnLogin.setEnabled(true);
        }
    }
}
