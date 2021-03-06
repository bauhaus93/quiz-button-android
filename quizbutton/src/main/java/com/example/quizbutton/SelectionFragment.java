package com.example.quizbutton;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.Timer;
import java.util.TimerTask;

public class SelectionFragment extends Fragment {

    private TextView textError;
    private final Timer timer = new Timer();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_selection, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textError = view.findViewById(R.id.textError);
        Bundle bundle = getArguments();
        if (bundle != null) {
            String error = bundle.getString("connection_error");
            if (error != null) {
                handleConnectionError(error);
                bundle.clear();
            }
        }

        QuizServer.destroy();


        view.findViewById(R.id.buttonHost).setOnClickListener(view1 -> {
            QuizServer.getInstance();
            NavHostFragment.findNavController(SelectionFragment.this)
                    .navigate(R.id.action_selectionFragment_to_buttonFragment);
        });
        view.findViewById(R.id.buttonClient).setOnClickListener(view1 -> NavHostFragment.findNavController(SelectionFragment.this)
                .navigate(R.id.action_selectionFragment_to_buttonFragment));
    }

    private void handleConnectionError(String error) {
        setErrorText(error);
        showErrorText();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                hideErrorText();
            }
        }, 5000);
    }

    private void setErrorText(String text) {
        textError.post(() -> textError.setText(text));
    }
    private void showErrorText() {
        textError.post(() -> textError.setVisibility(View.VISIBLE));
    }
    private void hideErrorText() {
        textError.post(() -> textError.setVisibility(View.INVISIBLE));
    }

}