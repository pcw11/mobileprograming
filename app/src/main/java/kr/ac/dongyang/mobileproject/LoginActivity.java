package kr.ac.dongyang.mobileproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import kr.ac.dongyang.mobileproject.database.DBHelper;
import kr.ac.dongyang.mobileproject.database.DBLogin;

public class LoginActivity extends AppCompatActivity {
    TextView enterId, enterPw;
    Button loginBtn;

    boolean loginTF = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. login.xml을 화면에 표시
        setContentView(R.layout.login); // R.layout.login.xml

        // 2. XML의 View들을 ID로 찾아 연결
        // '비밀번호 찾기' 텍스트뷰
        TextView findPasswordText = findViewById(R.id.tv_find_password);
        this.enterId = findViewById(R.id.et_id);
        this.enterPw = findViewById(R.id.et_password);
        // '회원가입' 텍스트뷰
        TextView registerText = findViewById(R.id.tv_register);
        this.loginBtn = findViewById(R.id.btn_login);
        TextWatcher loginTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // (사용 안 함)
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // (사용 안 함)
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 텍스트가 변경될 때마다 checkLoginFields() 메소드 호출
                chkEmpty();
            }
        };



        // 3. '비밀번호 찾기' 클릭 리스너 설정
        findPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // FindPasswordActivity로 이동하는 Intent
                Intent intent = new Intent(LoginActivity.this, FindPasswordActivity.class);
                startActivity(intent);
            }
        });

        // 4. '회원가입' 클릭 리스너 설정
        registerText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // RegisterActivity로 이동하는 Intent
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        enterId.addTextChangedListener(loginTextWatcher);
        enterPw.addTextChangedListener(loginTextWatcher);
        // (추가) 실제 로그인 버튼(btn_login) 등에도 리스너를 설정할 수 있습니다.

        loginBtn.setOnClickListener(v -> { //로그인 버튼 눌렀을때 동작
            String id = enterId.getText().toString();
            String pw = enterPw.getText().toString();
//            DBLogin dbLogin = new DBLogin();
//            loginTF = dbLogin.login(id, pw);
            loginTF = true; // 코드 작성 중 임시
            if (loginTF) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(LoginActivity.this, "아이디와 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();

            }
        });
    }


    @SuppressLint("ResourceAsColor")
    protected void chkEmpty(){
        if (this.enterId.getText().toString().isEmpty() || enterPw.getText().toString().isEmpty()) {
            loginBtn.setBackgroundResource(R.drawable.login_n_btn);
            loginBtn.setTextColor(R.color.gray);
            loginBtn.setEnabled(false);
        } else {
            loginBtn.setBackgroundResource(R.drawable.login_y_btn);
            loginBtn.setTextColor(R.color.white);
            loginBtn.setEnabled(true);
        }
    }
}