package app.nbs.kam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView profileName = view.findViewById(R.id.profile_name);

        // Ambil dari SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_pref", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "Pengguna");

        profileName.setText(username);

        return view;
    }
}