package de.dennisguse.opentracks;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import java.util.regex.Pattern;

import de.dennisguse.opentracks.databinding.LoginBinding;

public class SignupActivity extends AbstractActivity {

    private static final String EMAIL_REGEX_PATTERN = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
            + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX_PATTERN);
    private LoginBinding viewBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        TextView username = findViewById(R.id.username);
        TextView email = findViewById(R.id.email);
        TextView password = findViewById(R.id.password);
        TextView confirm = findViewById(R.id.confirm);

        if (!password.equals(confirm)) {
            throw new RuntimeException("The two passwords don't match!");
        }

        if (!EMAIL_PATTERN.matcher(email.getText().toString()).matches()) {
            throw new RuntimeException("The email is not valid!");
        }

        MaterialButton signupBtn = (MaterialButton) findViewById(R.id.signupBtn);
        
        signupBtn.setOnClickListener(v -> {

        });
    }

    @Override
    protected View getRootView() {
        viewBinding = LoginBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }
}
