package app.nbs.kam;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private View topToolbar;
    private BottomNavigationView bottomNavigationView;
    private TextView signOutButton;

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher, galleryActivityResultLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher, requestGalleryPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupActivityResultLaunchers();
        setupNavigationListener();
        setupSignOutListener();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DevicesFragment())
                    .commit();
            setNavbarGradient(R.drawable.gradient_top_bottom);
        }
    }

    private void initializeViews() {
        topToolbar = findViewById(R.id.top_toolbar_new);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        signOutButton = findViewById(R.id.signOutText);
    }

    private void setupSignOutListener() {
        if (signOutButton != null) {
            signOutButton.setOnClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Konfirmasi Logout")
                        .setMessage("Apakah kamu yakin ingin keluar?")
                        .setPositiveButton("Ya", (dialog, which) -> {
                            getSharedPreferences("user_pref", MODE_PRIVATE).edit().clear().apply();
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        }
    }

    private void setupNavigationListener() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (item.isChecked()) return false;

            Fragment selectedFragment = null;
            if (itemId == R.id.navigation_devices) {
                selectedFragment = new DevicesFragment();
                setNavbarGradient(R.drawable.gradient_top_bottom);
            } else if (itemId == R.id.navigation_camera) {
                selectedFragment = new CameraFragment();
                setNavbarGradient(R.drawable.gradient_top_bottom);
            } else if (itemId == R.id.navigation_history) {
                selectedFragment = new HistoryFragment();
                setNavbarGradient(R.drawable.gradient_history_background);
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
                setNavbarGradient(R.drawable.gradient_profile_background);
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }

    public void initiateImageCapture() {
        final CharSequence[] options = {"Ambil dari Kamera", "Pilih dari Galeri"};
        new AlertDialog.Builder(this)
                .setTitle("Pilih Sumber Gambar")
                .setItems(options, (dialog, item) -> {
                    if (item == 0) checkCameraPermissionAndOpenCamera();
                    else if (item == 1) checkGalleryPermissionAndOpenGallery();
                })
                .show();
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
                    if (imageBitmap != null) {
                        Uri imageUri = saveBitmapToCache(imageBitmap); // Simpan bitmap dan dapatkan URI
                        if (imageUri != null) {
                            processAndDisplayImageUri(imageUri); // Proses URI
                        }
                    }
                }
            }
        });

        galleryActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    processAndDisplayImageUri(imageUri); // Langsung proses URI
                }
            }
        });
    }

    // --- METODE BARU: Menyimpan bitmap ke file cache dan mengembalikan URI-nya ---
    private Uri saveBitmapToCache(Bitmap bitmap) {
        File cachePath = new File(getCacheDir(), "images");
        cachePath.mkdirs(); // Buat direktori jika belum ada
        try {
            File file = new File(cachePath, "image.jpg");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            return Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- METODE BARU: Mengirim URI (bukan bitmap) ke fragment ---
    private void processAndDisplayImageUri(Uri imageUri) {
        if (imageUri == null) return;

        DevicesFragment devicesFragment = new DevicesFragment();
        Bundle bundle = new Bundle();
        bundle.putString("captured_image_uri", imageUri.toString()); // Kirim URI sebagai String
        devicesFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, devicesFragment)
                .commit();

        // Update UI BottomNav tanpa memicu listener lagi
        bottomNavigationView.setOnItemSelectedListener(null);
        bottomNavigationView.setSelectedItemId(R.id.navigation_devices);
        setupNavigationListener();
    }

    // Metode lama processAndDisplayImage(Bitmap) bisa dihapus

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