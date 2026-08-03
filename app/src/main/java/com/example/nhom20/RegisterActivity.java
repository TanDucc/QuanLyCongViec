package com.example.nhom20;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton;
    private TextView loginText;
    private FirebaseAuth mAuth;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        initViews();
        setupListeners();
    }

    private void initViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loginText = findViewById(R.id.loginText);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> {
            v.setAlpha(0.7f);
            new Handler(Looper.getMainLooper()).postDelayed(() -> v.setAlpha(1f), 200);
            registerUser();
        });

        loginText.setOnClickListener(v -> navigateToLogin());
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!isInputValid(email, password, confirmPassword)) {
            return;
        }

        registerButton.setEnabled(false); // Vô hiệu hóa nút khi đang xử lý
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    registerButton.setEnabled(true); // Kích hoạt lại nút
                    if (task.isSuccessful()) {
                        showToast("Đăng ký thành công!");
                        navigateToLogin();
                    } else {
                        Log.e(TAG, "Đăng ký thất bại", task.getException());
                        showToast("Đăng ký thất bại: " + task.getException().getMessage());
                    }
                });
    }

    private boolean isInputValid(String email, String password, String confirmPassword) {
        if (email.isEmpty()) {
            showToast("Vui lòng nhập email!");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Email không hợp lệ!");
            return false;
        }
        if (password.length() < 6) {
            showToast("Mật khẩu phải có ít nhất 6 ký tự!");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showToast("Mật khẩu không khớp!");
            return false;
        }
        return true;
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}