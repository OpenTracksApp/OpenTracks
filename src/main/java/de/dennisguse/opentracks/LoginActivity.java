package de.dennisguse.opentracks;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.List;

import de.dennisguse.opentracks.data.models.Account;
import de.dennisguse.opentracks.data.models.AllAccounts;
import de.dennisguse.opentracks.databinding.LoginBinding;

public class LoginActivity extends AbstractActivity {

    private int loginCount;
    private static final int MAX_LOGIN_COUNT = 3;

    private LoginBinding viewBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
    }

    @Override
    protected View getRootView() {
        viewBinding = LoginBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    public void verifyUser(View view) {
        EditText usernameEditText = findViewById(R.id.username);
        EditText passwordEditText = findViewById(R.id.password);

        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        List<Account> accountList = AllAccounts.getAllAccounts();
        for (Account acc : accountList) {
            if (acc.getUsername().equals(username) && acc.getPassword().equals(password)) {
                Toast.makeText(LoginActivity.this, "Login Successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction("de.dennisguse.opentracks.introduction.IntroductionActivity");
                intent.putExtra("username", username);
                setResult(RESULT_OK, intent);
                finish();
                return;
            }
        }
        Toast.makeText(getApplicationContext(), "Username or password doesn't match!", Toast.LENGTH_SHORT).show();
        loginCount++;
        if (loginCount >= MAX_LOGIN_COUNT) {
            Toast.makeText(getApplicationContext(), "Maybe you need to sign up an account first!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
