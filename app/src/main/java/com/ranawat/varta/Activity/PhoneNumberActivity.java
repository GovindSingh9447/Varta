package com.ranawat.varta.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.ranawat.varta.R;
import com.ranawat.varta.databinding.ActivityPhoneNumberBinding;

public class PhoneNumberActivity extends AppCompatActivity {

 FirebaseAuth auth;
 EditText phoneBox;
    ActivityPhoneNumberBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPhoneNumberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        phoneBox = findViewById(R.id.phoneBox);

        getSupportActionBar().hide();


        auth =FirebaseAuth.getInstance();


        if (auth.getCurrentUser()!=null){
            Intent intent =new Intent(PhoneNumberActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        binding.phoneBox.requestFocus();

        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(PhoneNumberActivity.this , OtpActivity.class);
                intent.putExtra("phoneNumber", "+91" +phoneBox.getText().toString().trim());
                startActivity(intent);
            }
        });
    }
}