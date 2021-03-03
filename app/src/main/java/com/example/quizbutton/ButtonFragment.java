package com.example.quizbutton;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class ButtonFragment extends Fragment {

    private TextView statusText;
    private Button button;
    private QuizClient client;
    private Timer timer = new Timer();
    private MediaPlayer beep;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        beep = MediaPlayer.create(getActivity(), R.raw.airhorn);
        return inflater.inflate(R.layout.fragment_button, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusText = view.findViewById(R.id.textConnection);
        button = view.findViewById(R.id.buttonActivate);
        button.setOnTouchListener((view1, event) -> buttonCallback());
        startClient();
    }

    public void onStop() {
        super.onStop();
        if (client != null) {
            client.stop();
            client = null;
        }
    }

    private void startClient() {
        HostDiscovery discovery = new HostDiscovery(Constants.HOST_PORT);
        Thread discoverThread = new Thread(discovery);
        discoverThread.start();
        scheduleDiscoveryTasks(discovery);
    }

    private boolean buttonCallback() {
        if (client != null && client.sendButtonPressed()) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (client != null && client.isConnected()) {
                        int placement = client.getPlacement();
                        if (placement >= 1) {
                            setButtonText("#" + placement);
                            if (placement == 1) {
                                beep.start();
                            }
                            cancel();
                            scheduleButtonTextReset(Constants.TIME_CLIENT_PLACEMENT_RESET);
                        }
                    } else {
                        client = null;
                        cancel();
                    }
                }
            }, 100, 100);
        }
        return true;
    }

    private void setStatusText(String text) {
        statusText.post((Runnable) () -> {
            statusText.setText(text);
        });
    }

    private void setButtonText(String text) {
        button.post((Runnable) () -> {
            button.setText(text);
        });
    }

    private void clearButtonText() {
        setButtonText("");
    }

    private void scheduleButtonTextReset(long delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clearButtonText();
            }
        }, delay);
    }

    private void scheduleDiscoveryTasks(HostDiscovery discovery) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (discovery.foundHost()) {
                    client = new QuizClient(discovery.getHost());
                    Thread clientThread = new Thread(client);
                    clientThread.start();
                    setStatusText(discovery.getResultText());
                    cancel();
                } else if (!discovery.isActive()) {
                    client = null;
                    setStatusText(discovery.getResultText());
                    cancel();
                } else {
                    setStatusText(discovery.getResultText());
                }
            }
        }, 500, 500);
    }


}