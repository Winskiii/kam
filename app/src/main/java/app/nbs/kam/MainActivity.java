package app.nbs.kam; // Pastikan package name ini sesuai dengan proyek Anda

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder; // Untuk API 28+
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI Elements - Toolbar
    private TextView toolbarHelpButton, toolbarSignOutButton;

    // UI Elements - Map and Details
    private FrameLayout mapContainer;
    private ImageView detailsBackButton;
    private TextView textRoadNameValue, textDamageValue, textConfidenceValue;
    private LinearLayout detailsPanel;

    // UI Elements - Confidence Slider
    private SeekBar seekbarConfidence;
    private TextView textConfidenceSliderValue;
    private int currentConfidenceThreshold = 70;

    // UI Elements - Roboflow
    private ImageView roboflowImageViewNewUi;
    private TextView roboflowResultTextNewUi;
    private Bitmap currentPhoto;

    // UI Elements - Bottom Navigation
    private BottomNavigationView bottomNavigationView;

    // Roboflow API Details
    private static final String ROBOFLOW_API_URL_PREFIX = "https://detect.roboflow.com/";
    private static final String ROBOFLOW_MODEL_ENDPOINT = "road-damage-0cqzt/1"; // PASTIKAN INI BENAR
    private static final String ROBOFLOW_API_KEY = "GzmSCfORrjN5uttBwYNf";     // PASTIKAN INI BENAR

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> cameraActivityResultLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestGalleryPermissionLauncher; // Untuk izin galeri
    private ActivityResultLauncher<Intent> galleryActivityResultLauncher;  // Untuk memilih dari galeri

    private JSONArray allPredictions = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupActivityResultLaunchers();
        setupListeners();

        bottomNavigationView.setSelectedItemId(R.id.navigation_devices);
        showDevicesContent();
    }

    private void initializeViews() {
        toolbarHelpButton = findViewById(R.id.toolbar_help_button);
        toolbarSignOutButton = findViewById(R.id.toolbar_sign_out_button);

        mapContainer = findViewById(R.id.map_container);
        detailsPanel = findViewById(R.id.details_panel);
        detailsBackButton = findViewById(R.id.details_back_button);
        textRoadNameValue = findViewById(R.id.text_road_name_value);
        textDamageValue = findViewById(R.id.text_damage_value);
        textConfidenceValue = findViewById(R.id.text_confidence_value);

        seekbarConfidence = findViewById(R.id.seekbar_confidence);
        textConfidenceSliderValue = findViewById(R.id.text_confidence_slider_value);
        seekbarConfidence.setProgress(currentConfidenceThreshold);
        textConfidenceSliderValue.setText(String.format(Locale.getDefault(), getString(R.string.text_confidence_percentage), currentConfidenceThreshold));

        roboflowImageViewNewUi = findViewById(R.id.roboflow_image_view_new_ui);
        roboflowResultTextNewUi = findViewById(R.id.roboflow_result_text_new_ui);

        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
    }

    private void setupActivityResultLaunchers() {
        // Launcher untuk Izin Kamera
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openCamera();
            } else {
                Toast.makeText(this, getString(R.string.toast_camera_permission_denied), Toast.LENGTH_SHORT).show();
            }
        });

        // Launcher untuk Hasil Kamera
        cameraActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    processBitmap((Bitmap) extras.get("data"));
                } else {
                    Toast.makeText(this, getString(R.string.toast_failed_get_image_data_from_camera), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Launcher untuk Izin Galeri
        requestGalleryPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openGallery();
            } else {
                Toast.makeText(this, getString(R.string.toast_gallery_permission_denied), Toast.LENGTH_SHORT).show();
            }
        });

        // Launcher untuk Hasil Galeri
        galleryActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    try {
                        Bitmap bitmap;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageUri);
                            bitmap = ImageDecoder.decodeBitmap(source);
                        } else {
                            // Metode deprecated untuk API < 28
                            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        }
                        processBitmap(bitmap);
                    } catch (IOException e) {
                        Log.e(TAG, "Error loading image from gallery", e);
                        Toast.makeText(this, getString(R.string.toast_failed_load_image_from_gallery), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void processBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            currentPhoto = bitmap;
            roboflowImageViewNewUi.setImageBitmap(currentPhoto);
            roboflowImageViewNewUi.setVisibility(View.VISIBLE);
            roboflowResultTextNewUi.setText(getString(R.string.text_processing_image));
            roboflowResultTextNewUi.setVisibility(View.VISIBLE);
            sendToRoboflow(currentPhoto);
        } else {
            Toast.makeText(this, getString(R.string.toast_failed_get_image_from_camera), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        toolbarHelpButton.setOnClickListener(v -> Toast.makeText(MainActivity.this, getString(R.string.toast_help_clicked), Toast.LENGTH_SHORT).show());
        toolbarSignOutButton.setOnClickListener(v -> Toast.makeText(MainActivity.this, getString(R.string.toast_sign_out_clicked), Toast.LENGTH_SHORT).show());

        detailsBackButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, getString(R.string.toast_back_button_clicked), Toast.LENGTH_SHORT).show();
            showDevicesContent();
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_devices) {
                showDevicesContent();
                return true;
            } else if (itemId == R.id.navigation_camera) {
                showImageSourceDialog(); // Tampilkan dialog pilihan sumber
                return true;
            } else if (itemId == R.id.navigation_history) {
                showHistoryContent();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                showProfileContent();
                return true;
            }
            return false;
        });

        seekbarConfidence.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentConfidenceThreshold = progress;
                textConfidenceSliderValue.setText(String.format(Locale.getDefault(), getString(R.string.text_confidence_percentage), currentConfidenceThreshold));
                if (fromUser && allPredictions.length() > 0) {
                    filterAndDisplayPredictions();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "Confidence threshold set to: " + currentConfidenceThreshold + "%");
            }
        });
    }

    private void showImageSourceDialog() {
        // Panggil showCameraContent dulu untuk menyiapkan UI
        showCameraContent();

        CharSequence[] options = {getString(R.string.option_camera), getString(R.string.option_gallery)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_title_select_image_source));
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals(getString(R.string.option_camera))) {
                checkCameraPermissionAndOpenCamera();
            } else if (options[item].equals(getString(R.string.option_gallery))) {
                checkGalleryPermissionAndOpenGallery();
            }
        });
        builder.show();
    }

    // Metode untuk menampilkan konten berdasarkan tab yang dipilih (sama seperti sebelumnya)
    private void showDevicesContent() { /* ... kode sama ... */
        mapContainer.setVisibility(View.VISIBLE);
        detailsPanel.setVisibility(View.VISIBLE);
        roboflowImageViewNewUi.setVisibility(View.GONE);
        roboflowResultTextNewUi.setVisibility(View.GONE);
        Toast.makeText(MainActivity.this, getString(R.string.toast_devices_selected), Toast.LENGTH_SHORT).show();
        filterAndDisplayPredictions();
    }

    private void showCameraContent() { /* ... kode sama ... */
        mapContainer.setVisibility(View.GONE);
        detailsPanel.setVisibility(View.VISIBLE);
        roboflowImageViewNewUi.setVisibility(View.VISIBLE); // Siapkan untuk gambar
        roboflowResultTextNewUi.setVisibility(View.VISIBLE); // Siapkan untuk teks hasil
        roboflowResultTextNewUi.setText(getString(R.string.placeholder_detection_results));
        // Kosongkan gambar sebelumnya saat tab kamera dipilih ulang sebelum sumber dipilih
        roboflowImageViewNewUi.setImageDrawable(null);
        clearDamageDetails();
        allPredictions = new JSONArray();
        // Jangan panggil Toast di sini, akan dipanggil setelah sumber gambar dipilih
    }

    private void showHistoryContent() { /* ... kode sama ... */
        mapContainer.setVisibility(View.GONE);
        detailsPanel.setVisibility(View.GONE);
        roboflowImageViewNewUi.setVisibility(View.GONE);
        roboflowResultTextNewUi.setVisibility(View.GONE);
        Toast.makeText(MainActivity.this, getString(R.string.toast_history_selected), Toast.LENGTH_SHORT).show();
    }

    private void showProfileContent() { /* ... kode sama ... */
        mapContainer.setVisibility(View.GONE);
        detailsPanel.setVisibility(View.GONE);
        roboflowImageViewNewUi.setVisibility(View.GONE);
        roboflowResultTextNewUi.setVisibility(View.GONE);
        Toast.makeText(MainActivity.this, getString(R.string.toast_profile_selected), Toast.LENGTH_SHORT).show();
    }

    // Logika Kamera (sudah ada, hanya nama launcher izin yang disesuaikan)
    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, getString(R.string.camera_permission_needed_for_feature), Toast.LENGTH_LONG).show();
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraActivityResultLauncher.launch(cameraIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.toast_no_camera_app), Toast.LENGTH_SHORT).show();
        }
    }

    // Logika Galeri BARU
    private void checkGalleryPermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else { // Android 12 dan di bawahnya
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            Toast.makeText(this, getString(R.string.storage_permission_needed_for_gallery), Toast.LENGTH_LONG).show();
            requestGalleryPermissionLauncher.launch(permission);
        } else {
            requestGalleryPermissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Alternatif: Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT); galleryIntent.setType("image/*");
        try {
            galleryActivityResultLauncher.launch(galleryIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Tidak ada aplikasi galeri ditemukan.", Toast.LENGTH_SHORT).show(); // Sebaiknya string resource
        }
    }


    private void sendToRoboflow(Bitmap bitmap) { // ... kode sama seperti sebelumnya ...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        String inferenceApiUrl = ROBOFLOW_API_URL_PREFIX + ROBOFLOW_MODEL_ENDPOINT + "?api_key=" + ROBOFLOW_API_KEY;
        Log.d(TAG, "Sending image to: " + inferenceApiUrl);
        roboflowResultTextNewUi.setText(getString(R.string.text_processing_image));
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, inferenceApiUrl,
                response -> {
                    try {
                        Log.i(TAG, "Roboflow Raw Response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        allPredictions = jsonResponse.optJSONArray("predictions");
                        if (allPredictions == null) {
                            allPredictions = new JSONArray();
                        }
                        filterAndDisplayPredictions();
                    } catch (JSONException e) {
                        Log.e(TAG, "Roboflow JSON Parsing error: " + e.getMessage(), e);
                        roboflowResultTextNewUi.setText(getString(R.string.text_failed_parse_detection_result));
                        clearDamageDetails();
                    }
                },
                error -> {
                    String errorMessage = getString(R.string.text_failed_send_image_to_roboflow);
                    if (error.networkResponse != null) {
                        errorMessage += "\nStatus Code: " + error.networkResponse.statusCode;
                        Log.e(TAG, "VolleyError: " + error.toString() + " | Status Code: " + error.networkResponse.statusCode);
                        if (error.networkResponse.data != null) {
                            String responseBody = new String(error.networkResponse.data);
                            Log.e(TAG, "VolleyErrorDetail Body: " + responseBody);
                            errorMessage += "\nResponse: " + responseBody;
                        }
                    } else {
                        Log.e(TAG, "VolleyError: " + error.toString(), error);
                    }
                    roboflowResultTextNewUi.setText(errorMessage);
                    clearDamageDetails();
                }
        ) {
            @Override
            public String getBodyContentType() { return "application/x-www-form-urlencoded"; }
            @Override
            public byte[] getBody() throws AuthFailureError { return encodedImage.getBytes(); }
        };
        queue.add(stringRequest);
    }

    private void filterAndDisplayPredictions() { // ... kode sama seperti sebelumnya ...
        if (allPredictions.length() == 0) {
            roboflowResultTextNewUi.setText(getString(R.string.text_no_damage_detected));
            clearDamageDetails();
            return;
        }
        List<JSONObject> filteredPredictions = new ArrayList<>();
        double maxConfidence = 0;
        JSONObject bestPrediction = null;
        try {
            for (int i = 0; i < allPredictions.length(); i++) {
                JSONObject prediction = allPredictions.getJSONObject(i);
                double confidence = prediction.optDouble("confidence", 0.0) * 100;
                if (confidence >= currentConfidenceThreshold) {
                    filteredPredictions.add(prediction);
                    if (confidence > maxConfidence) {
                        maxConfidence = confidence;
                        bestPrediction = prediction;
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error filtering predictions: " + e.getMessage(), e);
            roboflowResultTextNewUi.setText(getString(R.string.text_failed_parse_detection_result));
            clearDamageDetails();
            return;
        }

        if (bestPrediction != null) {
            String className = bestPrediction.optString("class", "N/A");
            // Anda mungkin ingin normalisasi nama kelas di sini jika perlu
            // String normalizedClassName = className.toLowerCase().replace("_", " ");
            // if (normalizedClassName.equals("minimum damage") || ... )
            textDamageValue.setText(className); // Tampilkan kelas dari Roboflow apa adanya
            textConfidenceValue.setText(String.format(Locale.getDefault(), "%.0f%%", maxConfidence));
            textRoadNameValue.setText("Dari Gambar"); // Placeholder, bisa diupdate jika ada info lokasi
            StringBuilder resultBuilder = new StringBuilder();
            for(JSONObject pred : filteredPredictions){
                String predClass = pred.optString("class", "N/A");
                double predConf = pred.optDouble("confidence", 0.0) * 100;
                resultBuilder.append(String.format(Locale.getDefault(), getString(R.string.text_detection_result_format), predClass, predConf));
                resultBuilder.append("\n");
            }
            roboflowResultTextNewUi.setText(resultBuilder.toString().trim());
        } else {
            roboflowResultTextNewUi.setText(getString(R.string.text_no_damage_detected) + " (di atas " + currentConfidenceThreshold + "%)");
            clearDamageDetails();
        }
        roboflowResultTextNewUi.setVisibility(View.VISIBLE);
    }

    private void clearDamageDetails() { // ... kode sama seperti sebelumnya ...
        textRoadNameValue.setText(getString(R.string.placeholder_empty));
        textDamageValue.setText(getString(R.string.placeholder_empty));
        textConfidenceValue.setText(getString(R.string.placeholder_empty));
    }
}
