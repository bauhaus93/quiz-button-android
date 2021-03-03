package com.example.quizbutton;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Timer;
import java.util.TimerTask;

public class ButtonFragment extends Fragment {

    private TextView statusText;
    private Button button;
    private QuizClient client;
    private final Timer timer = new Timer();
    private TimerTask resetColorTask;
    private TimerTask resetTextTask;
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
                            setButtonColor(getResources().getColor(R.color.buttonActive, null));
                            if (placement == 1) {
                                beep.start();
                            }
                            cancel();
                            scheduleButtonColorReset();
                            scheduleButtonTextReset();
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
        statusText.post(() -> statusText.setText(text));
    }

    private void setButtonText(String text) {
        button.post(() -> button.setText(text));
    }

    private void clearButtonText() {
        setButtonText("");
    }

    private void setButtonColor(int color) {
        button.post(() -> button.setBackgroundColor(color));
    }

    private void scheduleButtonTextReset() {
        if (resetTextTask != null) {
            resetTextTask.cancel();
        }
        resetTextTask = new TimerTask() {
            @Override
            public void run() {
               clearButtonText();
               resetTextTask = null;
            }
        };
        timer.schedule(resetTextTask, Constants.TIME_CLIENT_PLACEMENT_RESET);
    }

    private void scheduleButtonColorReset() {
        if (resetColorTask != null) {
            resetColorTask.cancel();
        }
        resetColorTask = new TimerTask() {
            @Override
            public void run() {
                setButtonColor(getResources().getColor(R.color.buttonDefault, null));
                resetColorTask = null;
            }
        };
        timer.schedule(resetColorTask
        , Constants.TIME_BETWEEN_ACTIVATION);
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