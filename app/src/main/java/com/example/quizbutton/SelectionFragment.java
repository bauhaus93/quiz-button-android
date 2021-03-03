package com.example.quizbutton;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class SelectionFragment extends Fragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_selection, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity)getActivity()).stopServer();

        view.findViewById(R.id.buttonHost).setOnClickListener(view1 -> {
            ((MainActivity)getActivity()).startServer();
            NavHostFragment.findNavController(SelectionFragment.this)
                    .navigate(R.id.action_selectionFragment_to_buttonFragment);
        });
        view.findViewById(R.id.buttonClient).setOnClickListener(view1 -> NavHostFragment.findNavController(SelectionFragment.this)
                .navigate(R.id.action_selectionFragment_to_buttonFragment));
    }
}