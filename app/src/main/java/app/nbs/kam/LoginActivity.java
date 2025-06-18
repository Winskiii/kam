package app.nbs.kam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText usernameInput = findViewById(R.id.usernameInput);
        Button btnLoginConfirm = findViewById(R.id.btnLoginConfirm);

        btnLoginConfirm.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();

            // Simpan ke SharedPreferences
            SharedPreferences prefs = getSharedPreferences("user_pref", MODE_PRIVATE);
            prefs.edit().putString("username", username).apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}