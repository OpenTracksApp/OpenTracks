package de.dennisguse.opentracks;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import de.dennisguse.opentracks.databinding.LoginBinding;

public class LoginActivity extends AbstractActivity {

    private LoginBinding viewBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        TextView username = findViewById(R.id.username);
        TextView password = findViewById(R.id.password);

        MaterialButton loginBtn = (MaterialButton) findViewById(R.id.loginBtn);

        // admin and admin
        loginBtn.setOnClickListener(v -> {
            if (username.getText().toString().equals("admin") && password.getText().toString().equals("admin")) {
                Toast.makeText(LoginActivity.this, "Login Successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LoginActivity.this, "Login Failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected View getRootView() {
        viewBinding = LoginBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }
}
