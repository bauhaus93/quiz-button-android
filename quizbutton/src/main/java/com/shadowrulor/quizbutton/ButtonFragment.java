package com.shadowrulor.quizbutton;

import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.Timer;
import java.util.TimerTask;

public class ButtonFragment extends Fragment {

    private TextView statusText;
    private Button button;
    private QuizClient client;
    private final Timer timer = new Timer();
    private TimerTask resetColorTask;
    private TimerTask resetTextTask;
    private TimerTask discoveryTask;
    private TimerTask connectionCheckTask;
    private MediaPlayer beep;

    @Override
    public void onStart() {
        super.onStart();
        startClient();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (client != null) {
            client.stop();
            client = null;
        }
    }
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        beep = MediaPlayer.create(getActivity(), R.raw.airhorn);
        return inflater.inflate(R.layout.fragment_button, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (beep != null) {
            beep.release();
        }
        if (resetColorTask != null) {
            resetColorTask.cancel();
        }
        if (resetTextTask != null) {
            resetTextTask.cancel();
        }
        if (discoveryTask != null) {
            discoveryTask.cancel();
        }
        if (connectionCheckTask != null) {
            connectionCheckTask.cancel();
        }
        QuizServer.destroy();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusText = view.findViewById(R.id.textConnection);
        button = view.findViewById(R.id.buttonActivate);
        button.setOnTouchListener((view1, event) -> buttonCallback());
    }

    private void startClient() {
        HostDiscovery discovery = new HostDiscovery(Constants.HOST_PORT);
        Thread discoverThread = new Thread(discovery);
        discoverThread.start();

        scheduleDiscoveryTasks(discovery);
        setStatusText(getResources().getString(R.string.host_discovery_active));
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
                        Bundle bundle = new Bundle();
                        bundle.putString("connection_error", getResources().getString(R.string.not_connected));
                        Navigation.findNavController(getView()).navigate(R.id.action_buttonFragment_to_selectionFragment, bundle);
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

    private void setStatusTextColor(int color) {
        statusText.post(() -> statusText.setTextColor(color));
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

    private void scheduleConnectionCheckTask() {
        connectionCheckTask = new TimerTask() {
            @Override
            public void run() {
                Resources res = getResources();
                View view = getView();
                if (view != null) {
                    if(QuizServer.hasStopped()) {
                        Bundle bundle = new Bundle();
                        bundle.putString("connection_error", res != null ? res.getString(R.string.host_error):"Host error");
                        Navigation.findNavController(view).navigate(R.id.action_buttonFragment_to_selectionFragment, bundle);
                    } else if (client == null || !client.isConnected()) {
                        Bundle bundle = new Bundle();
                        bundle.putString("connection_error", res != null ? res.getString(R.string.disconnected):"Client disconnected");
                        Navigation.findNavController(view).navigate(R.id.action_buttonFragment_to_selectionFragment, bundle);
                    }
                }
            }
        };
        timer.schedule(connectionCheckTask, Constants.CONNECTIVITY_CHECK_INTERVAL, Constants.CONNECTIVITY_CHECK_INTERVAL);
    }

    private void scheduleDiscoveryTasks(HostDiscovery discovery) {
        discoveryTask = new TimerTask() {
            @Override
            public void run() {
                ConnectionState state = discovery.getState();
                switch(discovery.getState()) {
                    case CONNECTED:
                        client = new QuizClient(discovery.getHost());
                        Thread clientThread = new Thread(client);
                        clientThread.start();
                        setStatusText(getResources().getString(R.string.connected));
                        cancel();
                        scheduleConnectionCheckTask();
                        break;
                    case NO_HOST_FOUND:
                    case NO_WIFI_FOUND:
                        Bundle bundle = new Bundle();
                        String error;
                        if (state == ConnectionState.NO_WIFI_FOUND) {
                            error = getResources().getString(R.string.no_wifi_found);
                        } else {
                            error = getResources().getString(R.string.no_host_found);
                        }
                        bundle.putString("connection_error", error);
                        Navigation.findNavController(getView()).navigate(R.id.action_buttonFragment_to_selectionFragment, bundle);
                        client = null;

                        setStatusTextColor(getResources().getColor(R.color.red, null));
                        cancel();
                        break;
                    default:
                        break;
                }
            }
        };
        timer.schedule(discoveryTask, 500, 500);
    }


}