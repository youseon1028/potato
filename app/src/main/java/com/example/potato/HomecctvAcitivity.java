package com.example.potato;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class HomecctvAcitivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homecctv_acitivity);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.0.12:81/stream"));
        startActivity(intent);

        finish();

//        Button Button2 = (Button) findViewById(R.id.button2);
//        Button2.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
//                startActivity(intent);
//            }
//        });
//
//
//        Button button8 = (Button) findViewById(R.id.button8);
//        button8.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.0.12/"));
//                startActivity(intent);
//            }
//        });

    }
}