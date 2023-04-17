package de.dennisguse.opentracks;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import de.dennisguse.opentracks.data.models.Account;
import de.dennisguse.opentracks.data.models.AllAccounts;
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
    }

    @Override
    protected View getRootView() {
        viewBinding = LoginBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    public void registerUser(View view) {
        TextView usernameTextView = findViewById(R.id.username);
        TextView emailTextView = findViewById(R.id.email);
        TextView passwordTextView = findViewById(R.id.password);
        TextView confirmTextView = findViewById(R.id.confirm);

        String username = usernameTextView.getText().toString();
        String password = passwordTextView.getText().toString();
        String confirm = confirmTextView.getText().toString();
        String email = emailTextView.getText().toString();

        if (!password.equals(confirm)) {
            Toast.makeText(getApplicationContext(), "The two passwords don't match!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            Toast.makeText(getApplicationContext(), "The email is not valid!", Toast.LENGTH_SHORT).show();
            return;
        }

        AllAccounts.registerAccount(new Account(username, email, password));

        Intent intent = new Intent();
        intent.setAction("de.dennisguse.opentracks.LoginActivity");
        intent.putExtra("username", username);
        setResult(RESULT_OK, intent);
        finish();
    }
}
