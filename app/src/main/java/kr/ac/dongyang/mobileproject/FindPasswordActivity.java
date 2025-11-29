package kr.ac.dongyang.mobileproject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import kr.ac.dongyang.mobileproject.databinding.FindpwBinding;

public class FindPasswordActivity extends AppCompatActivity {

    private FindpwBinding binding;
    private boolean isVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FindpwBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 초기에는 비밀번호 필드와 변경 버튼을 비활성화
        binding.etPassword.setEnabled(false);
        binding.etPasswordConfirm.setEnabled(false);
        binding.btnRegister.setEnabled(false);

        // 인증 버튼 클릭 리스너
        binding.btnVerifyEmail.setOnClickListener(v -> verifyUser());

        // 변경하기 버튼 클릭 리스너
        binding.btnRegister.setOnClickListener(v -> changePassword());

        // 비밀번호 유효성 검사
        addTextWatchers();
    }

    private void verifyUser() {
        String id = binding.etId.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();

        if (id.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "아이디와 이메일을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            boolean userExists = false;
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                String sql = "SELECT 1 FROM users WHERE user_id = ? AND email = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, id);
                    pstmt.setString(2, email);
                    ResultSet rs = pstmt.executeQuery();
                    userExists = rs.next(); // 백그라운드 스레드에서 결과를 확인합니다.
                }
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "데이터베이스 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
                return; // 오류 발생 시 스레드 종료
            }

            // 확인된 결과를 메인 스레드로 전달하여 UI를 업데이트합니다.
            boolean finalUserExists = userExists;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalUserExists) {
                    isVerified = true;
                    Toast.makeText(this, "인증에 성공했습니다. 새 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    binding.etId.setEnabled(false);
                    binding.etEmail.setEnabled(false);
                    binding.btnVerifyEmail.setEnabled(false);
                    binding.etPassword.setEnabled(true);
                    binding.etPasswordConfirm.setEnabled(true);
                    binding.btnRegister.setEnabled(true);
                } else {
                    Toast.makeText(this, "일치하는 사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void changePassword() {
        if (!isVerified) {
            Toast.makeText(this, "먼저 이메일 인증을 완료해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String password = binding.etPassword.getText().toString().trim();
        String passwordConfirm = binding.etPasswordConfirm.getText().toString().trim();

        if (password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "새 비밀번호와 비밀번호 확인을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "비밀번호는 8자 이상이며, 숫자와 특수문자를 포함해야 합니다.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = binding.etId.getText().toString().trim();

        new Thread(() -> {
            try (Connection conn = new DatabaseConnector(this).getConnection()) {
                String sql = "UPDATE users SET password = ? WHERE user_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, password);
                    pstmt.setString(2, id);
                    int updatedRows = pstmt.executeUpdate();

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (updatedRows > 0) {
                            Toast.makeText(this, "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show();
                            finish(); // 로그인 화면으로 돌아가기
                        } else {
                            Toast.makeText(this, "비밀번호 변경에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, "데이터베이스 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasDigit = false;
        boolean hasSpecialChar = false;

        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecialChar = true;
            }
        }

        return hasDigit && hasSpecialChar;
    }

    private void addTextWatchers() {
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isValidPassword(s.toString())) {
                    binding.tvPwStatus.setText("✅");
                } else {
                    binding.tvPwStatus.setText("❌");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals(binding.etPassword.getText().toString())) {
                    binding.tvPwConfirmStatus.setText("✅");
                } else {
                    binding.tvPwConfirmStatus.setText("❌");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
