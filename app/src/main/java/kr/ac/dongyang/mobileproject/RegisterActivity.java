package kr.ac.dongyang.mobileproject;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.MotionEvent;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import kr.ac.dongyang.mobileproject.databinding.RegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private RegisterBinding binding;
    private boolean isIdChecked = false;
    private boolean isPasswordValid = false;
    private boolean isPasswordConfirmed = false;
    private boolean isEmailValid = false;
    private boolean emailToastShown = false;
    private boolean isCheckingAll = false; // Flag to prevent listener loops


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = RegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set initial password visibility icon
        binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0);
        binding.etPasswordConfirm.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0);

        setupListeners();
        updateRegisterButtonState();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupListeners() {
        // 아이디 중복확인 버튼 클릭 리스너
        binding.btnCheckDuplicate.setOnClickListener(v -> {
            String userId = binding.etId.getText().toString().trim();
            if (userId.isEmpty()) {
                Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            checkIdDuplicate(userId);
        });

        // 가입하기 버튼 클릭 리스너
        binding.btnRegister.setOnClickListener(v -> {
            registerUser(
                    binding.etId.getText().toString().trim(),
                    binding.etPassword.getText().toString().trim(),
                    binding.etEmail.getText().toString().trim()
            );
        });

        // 아이디 입력 변경 시
        binding.etId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isIdChecked = false;
                updateRegisterButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 비밀번호 입력 변경 시
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                validatePassword();
                validatePasswordConfirmation();
                updateRegisterButtonState();
            }
        });

        binding.etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (binding.etPassword.getRight() - binding.etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility(binding.etPassword);
                    return true;
                }
            }
            return false;
        });

        // 비밀번호 확인 입력 변경 시
        binding.etPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                validatePasswordConfirmation();
                updateRegisterButtonState();
            }
        });

        binding.etPasswordConfirm.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (binding.etPasswordConfirm.getRight() - binding.etPasswordConfirm.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    togglePasswordVisibility(binding.etPasswordConfirm);
                    return true;
                }
            }
            return false;
        });

        // 이메일 입력 변경 시
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                validateEmail();
                updateRegisterButtonState();
            }
        });

        // 약관 동의 체크박스 리스너
        binding.cbAgreeAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isCheckingAll = true;
            binding.cbAgreeTerms.setChecked(isChecked);
            binding.cbAgreePrivacy.setChecked(isChecked);
            binding.cbAgreeAds.setChecked(isChecked);
            isCheckingAll = false;
            updateRegisterButtonState();
        });

        CompoundButton.OnCheckedChangeListener individualCheckboxListener = (buttonView, isChecked) -> {
            if (isCheckingAll) return; // Prevent loop

            if (binding.cbAgreeTerms.isChecked() && binding.cbAgreePrivacy.isChecked() && binding.cbAgreeAds.isChecked()) {
                binding.cbAgreeAll.setChecked(true);
            } else {
                binding.cbAgreeAll.setChecked(false);
            }
            updateRegisterButtonState();
        };

        binding.cbAgreeTerms.setOnCheckedChangeListener(individualCheckboxListener);
        binding.cbAgreePrivacy.setOnCheckedChangeListener(individualCheckboxListener);
        binding.cbAgreeAds.setOnCheckedChangeListener(individualCheckboxListener);
    }

    private void togglePasswordVisibility(EditText editText) {
        Typeface typeface = editText.getTypeface();
        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_on, 0);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0);
        }
        editText.setTypeface(typeface);
        editText.setSelection(editText.getText().length());
    }
    // TODO 회원가입 비밀번호 검증 오류 수정
    private void validatePassword() {
        String password = binding.etPassword.getText().toString().trim();
        // 8자 이상, 영문, 숫자, 특수문자 포함
        Pattern passwordPattern = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{8,}$");
        isPasswordValid = passwordPattern.matcher(password).matches();
        binding.tvPwStatus.setText(isPasswordValid ? "✅" : "❌");
    }

    private void validatePasswordConfirmation() {
        String password = binding.etPassword.getText().toString().trim();
        String passwordConfirm = binding.etPasswordConfirm.getText().toString().trim();
        isPasswordConfirmed = !password.isEmpty() && password.equals(passwordConfirm);
        binding.tvPwConfirmStatus.setText(isPasswordConfirmed ? "✅" : "❌");
    }

    private void validateEmail() {
        String email = binding.etEmail.getText().toString().trim();
        isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        if (isEmailValid && !emailToastShown) {
            Toast.makeText(this, "사용 가능한 이메일입니다.", Toast.LENGTH_SHORT).show();
            emailToastShown = true;
        } else if (!isEmailValid) {
            emailToastShown = false;
        }
    }


    private void updateRegisterButtonState() {
        boolean allConditionsMet = isIdChecked && isPasswordValid && isPasswordConfirmed && isEmailValid && binding.cbAgreeTerms.isChecked() && binding.cbAgreePrivacy.isChecked();
        binding.btnRegister.setEnabled(allConditionsMet);
        if (allConditionsMet) {
            binding.btnRegister.setBackgroundColor(Color.parseColor("#4CFF7F"));
            binding.btnRegister.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            binding.btnRegister.setBackgroundResource(R.drawable.round_edge_gray);
            binding.btnRegister.setTextColor(ContextCompat.getColor(this, R.color.register_button_text_disabled));
        }
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
                conn = new DatabaseConnector(this).getConnection();
                String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, userId);
                rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    isDuplicate = true;
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
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
                updateRegisterButtonState();
            });
        });
    }

    private void registerUser(final String userId, final String password, final String email) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            boolean success = false;
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = new DatabaseConnector(this).getConnection();
                String sql = "INSERT INTO users (user_id, password, email) VALUES (?, ?, ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, userId);
                pstmt.setString(2, PasswordHasher.hashPassword(password));
                pstmt.setString(3, email);
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    success = true;
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (pstmt != null) pstmt.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            boolean finalSuccess = success;
            handler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, "회원가입에 성공했습니다!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
