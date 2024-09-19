package com.example.grabi;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
public class Testmode extends AppCompatActivity {
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testmode);
        Button BlinkButton=findViewById(R.id.BlinkButton);
        Button RednessButton=findViewById(R.id.RednessButton);
        BlinkButton.setOnClickListener(v -> {
          Intent blink=new Intent(this,Blinktest.class);
          startActivity(blink);

        });
        RednessButton.setOnClickListener(v -> {
            Intent redness=new Intent(this,InstActivity.class);
            startActivity(redness);

        });

    }
}
