package com.example.quizbutton;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private QuizServer server;
    private final Timer timer = new Timer();

    public void startServer() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                server = new QuizServer();
                Thread thread = new Thread(server);
                thread.start();
            }
        }, 0);
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopServer();
    }
}
