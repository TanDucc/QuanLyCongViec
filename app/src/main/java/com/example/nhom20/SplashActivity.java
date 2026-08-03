package com.example.nhom20;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Gán layout Splash

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        long startTime = System.currentTimeMillis();

        new Handler().postDelayed(() -> {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long remainingDelay = Math.max(0, 2000 - elapsedTime); // Tối thiểu 2 giây

            new Handler().postDelayed(() -> {
                try {
                    if (currentUser != null) {
                        startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                    } else {
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    }
                } catch (Exception e) {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }, remainingDelay);
        }, 0);
    }
}