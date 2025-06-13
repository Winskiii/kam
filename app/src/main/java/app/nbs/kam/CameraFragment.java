package app.nbs.kam;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CameraFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button takePictureButton = view.findViewById(R.id.button_take_photo);
        takePictureButton.setOnClickListener(v -> {
            // Beri tahu MainActivity untuk memulai proses pengambilan gambar
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).initiateImageCapture();
            }
        });
    }
}