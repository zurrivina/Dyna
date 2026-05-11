package com.flying_kiwi.dyna;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.flying_kiwi.dyna.Utils.FileManager;

import java.util.ArrayList;

public class SwapProfile extends Fragment {
    //TODO This whole activity will probably eventually become a dropdown on the main page
    View view;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);

        view = inflater.inflate(R.layout.swap_profile_fragment,container, false);
        addButtons();
        NavController navController = Navigation.findNavController(requireActivity(), R.id.fragmentContainerView);
        view.findViewById(R.id.btnCreateProfile).setOnClickListener(v ->{
            navController.navigate(R.id.action_swapProfile_to_createProfile);
        });
        return view;
    }
    public void addButtons(){
        FileManager fm = new FileManager(requireContext());
        LinearLayout llProfiles = view.findViewById(R.id.llProfiles);
        llProfiles.removeAllViews();
        ArrayList<Profile> profiles = fm.getAllProfiles();
        for(Profile p : profiles){
            llProfiles.addView(getProfileRow(p));
        }
    }

    private String getActiveUserName() {
        return requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE)
                .getString("ActiveUser", "Default");
    }

    private @NonNull View getProfileRow(Profile profile) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button profileButton = new Button(requireContext());
        profileButton.setText(profile.getDisplayName());
        profileButton.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        NavController navController = Navigation.findNavController(requireActivity(), R.id.fragmentContainerView);
        profileButton.setOnClickListener(v -> {
            changeUser(profile.getName());
            navController.popBackStack();
        });
        row.addView(profileButton);

        Button deleteButton = new Button(requireContext());
        deleteButton.setText("X");
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        deleteButton.setOnClickListener(v -> confirmDelete(profile));
        row.addView(deleteButton);

        return row;
    }

    private void confirmDelete(Profile profile) {
        String activeUser = getActiveUserName();
        if (profile.getName().equals(activeUser)) {
            Toast.makeText(requireContext(), "Cannot delete active profile", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Delete \"" + profile.getDisplayName() + "\" and all its data?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FileManager fm = new FileManager(requireContext());
                    fm.deleteProfile(profile);
                    addButtons();
                    Toast.makeText(requireContext(), "Profile deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void changeUser(String userName) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("ActiveUser", userName);
        editor.apply();
        ((Button)getActivity().findViewById(R.id.btnProfile)).setText(userName);
    }
}