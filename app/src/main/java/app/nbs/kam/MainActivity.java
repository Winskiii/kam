package app.nbs.kam;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private View topToolbar;
    private BottomNavigationView bottomNavigationView;

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher, galleryActivityResultLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher, requestGalleryPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupActivityResultLaunchers();
        setupNavigationListener();

        if (savedInstanceState == null) {
            // Muat DevicesFragment sebagai tampilan awal
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DevicesFragment())
                    .commit();
            setNavbarGradient(R.drawable.gradient_top_bottom); // Set gradasi awal
        }
    }

    private void initializeViews() {
        topToolbar = findViewById(R.id.top_toolbar_new);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
    }

    // --- PERBAIKAN UTAMA DI SINI ---
    private void setupNavigationListener() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            // Jangan buat fragment baru jika item yang diklik sudah aktif
            if (item.isChecked()) {
                return false;
            }

            if (itemId == R.id.navigation_devices) {
                selectedFragment = new DevicesFragment(); // Buat instance baru hanya saat dipilih manual
                setNavbarGradient(R.drawable.gradient_top_bottom);
            } else if (itemId == R.id.navigation_camera) {
                selectedFragment = new CameraFragment();
                setNavbarGradient(R.drawable.gradient_top_bottom);
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
                setNavbarGradient(R.drawable.gradient_profile_background);
            }
            // else if (itemId == R.id.navigation_history) { ... }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment, String.valueOf(itemId)) // Beri tag unik
                        .commit();
                return true;
            }
            return false;
        });
    }

    // --- PERBAIKAN KEDUA DI SINI ---
    private void processAndDisplayImage(Bitmap bitmap) {
        if (bitmap == null) return;

        // 1. Buat instance baru dari DevicesFragment
        DevicesFragment devicesFragment = new DevicesFragment();

        // 2. Siapkan "paket" (Bundle) untuk mengirim data
        Bundle bundle = new Bundle();
        bundle.putParcelable("captured_image", bitmap);
        devicesFragment.setArguments(bundle);

        // 3. Ganti fragment di container dengan fragment yang sudah berisi data
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, devicesFragment)
                .commit();

        // 4. HANYA UPDATE TAMPILAN NAVIGASI, JANGAN PICU LISTENER LAGI
        // Matikan listener sementara, ubah item terpilih, lalu nyalakan lagi
        bottomNavigationView.setOnItemSelectedListener(null);
        bottomNavigationView.setSelectedItemId(R.id.navigation_devices);
        setupNavigationListener(); // Pasang kembali listener untuk penggunaan normal
    }

    public void initiateImageCapture() {
        final CharSequence[] options = {"Ambil dari Kamera", "Pilih dari Galeri"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Sumber Gambar");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Ambil dari Kamera")) {
                checkCameraPermissionAndOpenCamera();
            } else if (options[item].equals("Pilih dari Galeri")) {
                checkGalleryPermissionAndOpenGallery();
            }
        });
        builder.show();
    }

    private void setupActivityResultLaunchers() {
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) openCamera();
            else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show();
        });

        requestGalleryPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) openGallery();
            else Toast.makeText(this, "Izin galeri ditolak", Toast.LENGTH_SHORT).show();
        });

        cameraActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    processAndDisplayImage(imageBitmap);
                }
            }
        });

        galleryActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    try {
                        Bitmap bitmap = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ?
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.getContentResolver(), imageUri)) :
                                MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        processAndDisplayImage(bitmap);
                    } catch (IOException e) {
                        Log.e("MainActivity", "Gagal memuat gambar dari galeri", e);
                    }
                }
            }
        });
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraActivityResultLauncher.launch(takePictureIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Aplikasi kamera tidak ditemukan.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkGalleryPermissionAndOpenGallery() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestGalleryPermissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryActivityResultLauncher.launch(pickPhoto);
    }

    private void setNavbarGradient(int drawableResId) {
        Drawable gradient = ContextCompat.getDrawable(this, drawableResId);
        if (topToolbar != null) topToolbar.setBackground(gradient);
        if (bottomNavigationView != null) bottomNavigationView.setBackground(gradient);
    }
}