package com.real.shealthex01;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Mental extends AppCompatActivity {
    public LinearLayout mental_view;
    public TextView status;
    public TextView play;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mental);

        mental_view = findViewById(R.id.mental_view);
        status = findViewById(R.id.status);
        play = findViewById(R.id.play);

        float value = ((MainActivity)MainActivity.value_temp).value;

        if(value<66) {
            mental_view.setBackgroundResource(android.R.color.holo_blue_dark);
            status.setText("좋음");
        } else if(value>=66 &&  value<=82) {
            mental_view.setBackgroundResource(android.R.color.holo_green_dark);
            status.setText("보통");
        } else if(value>82 && value<=85) {
            mental_view.setBackgroundResource(android.R.color.holo_orange_dark);
            status.setText("나쁨");
        } else if(value>85) {
            mental_view.setBackgroundResource(android.R.color.holo_red_dark);
            status.setText("매우 나쁨");
        } else {
            mental_view.setBackgroundResource(android.R.color.black);
            status.setText("오류");
        }

        play.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), com.real.shealthex01.Play.class);
                startActivity(intent);
            }
        });
    }
}