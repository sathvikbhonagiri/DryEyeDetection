package com.example.grabi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class InstActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inst);
        Button agree=(Button)findViewById(R.id.agree);
        TextView cont=(TextView)findViewById(R.id.cont);
        TextView hdn=(TextView)findViewById(R.id.hdn);
        ImageView ref=(ImageView)findViewById(R.id.ref);
        agree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(InstActivity.this, RedcamActivity.class);
                startActivity(mainIntent);
            }
        });

    }
}

