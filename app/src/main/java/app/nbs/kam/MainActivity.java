package app.nbs.kam; // Pastikan package name ini sesuai dengan proyek Anda

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar; // Import SeekBar
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.ArrayList; // Untuk menyimpan prediksi
import java.util.List;    // Untuk menyimpan prediksi
import java.util.Locale;  // Untuk formatting string

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
    private int currentConfidenceThreshold = 70; // Default confidence threshold

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
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Menyimpan semua prediksi dari Roboflow sebelum difilter
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

        // Confidence Slider
        seekbarConfidence = findViewById(R.id.seekbar_confidence);
        textConfidenceSliderValue = findViewById(R.id.text_confidence_slider_value);
        seekbarConfidence.setProgress(currentConfidenceThreshold); // Set nilai awal
        textConfidenceSliderValue.setText(String.format(Locale.getDefault(),getString(R.string.text_confidence_percentage), currentConfidenceThreshold));


        roboflowImageViewNewUi = findViewById(R.id.roboflow_image_view_new_ui);
        roboflowResultTextNewUi = findViewById(R.id.roboflow_result_text_new_ui);

        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
    }

    private void setupActivityResultLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openCamera();
            } else {
                Toast.makeText(this, getString(R.string.toast_camera_permission_denied), Toast.LENGTH_SHORT).show();
            }
        });

        cameraActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    currentPhoto = (Bitmap) extras.get("data");
                    if (currentPhoto != null) {
                        roboflowImageViewNewUi.setImageBitmap(currentPhoto);
                        roboflowImageViewNewUi.setVisibility(View.VISIBLE);
                        roboflowResultTextNewUi.setText(getString(R.string.text_processing_image));
                        roboflowResultTextNewUi.setVisibility(View.VISIBLE);
                        sendToRoboflow(currentPhoto);
                    } else {
                        Toast.makeText(this, getString(R.string.toast_failed_get_image_from_camera), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.toast_failed_get_image_data_from_camera), Toast.LENGTH_SHORT).show();
                }
            }
        });
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
                showCameraContent();
                checkCameraPermissionAndOpenCamera();
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

        // Listener untuk SeekBar Confidence
        seekbarConfidence.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentConfidenceThreshold = progress;
                textConfidenceSliderValue.setText(String.format(Locale.getDefault(), getString(R.string.text_confidence_percentage), currentConfidenceThreshold));
                // Jika Anda ingin langsung memfilter ulang hasil yang sudah ada saat slider diubah:
                if (allPredictions.length() > 0) {
                    filterAndDisplayPredictions();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Tidak perlu aksi khusus
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Tidak perlu aksi khusus, atau bisa memicu pemfilteran di sini jika tidak di onProgressChanged
                Log.d(TAG, "Confidence threshold set to: " + currentConfidenceThreshold + "%");
            }
        });
    }

    private void showDevicesContent() {
        mapContainer.setVisibility(View.VISIBLE);
        detailsPanel.setVisibility(View.VISIBLE);
        roboflowImageViewNewUi.setVisibility(View.GONE);
        roboflowResultTextNewUi.setVisibility(View.GONE);
        Toast.makeText(MainActivity.this, getString(R.string.toast_devices_selected), Toast.LENGTH_SHORT).show();
        // Di sini Anda bisa menampilkan data terakhir yang valid (jika ada) atau membersihkan detail
        filterAndDisplayPredictions(); // Tampilkan data terakhir dengan threshold saat ini
    }

    private void showCameraContent() {
        mapContainer.setVisibility(View.GONE);
        detailsPanel.setVisibility(View.VISIBLE); // Panel detail digunakan untuk hasil kamera
        roboflowImageViewNewUi.setVisibility(View.VISIBLE);
        roboflowResultTextNewUi.setVisibility(View.VISIBLE);
        roboflowResultTextNewUi.setText(getString(R.string.placeholder_detection_results)); // Reset teks hasil
        clearDamageDetails(); // Bersihkan detail untuk tangkapan baru
        allPredictions = new JSONArray(); // Reset prediksi sebelumnya
        Toast.makeText(MainActivity.this, getString(R.string.toast_camera_selected), Toast.LENGTH_SHORT).show();
    }

    private void showHistoryContent() {
        mapContainer.setVisibility(View.GONE);
        detailsPanel.setVisibility(View.GONE);
        roboflowImageViewNewUi.setVisibility(View.GONE);
        roboflowResultTextNewUi.setVisibility(View.GONE);
        Toast.makeText(MainActivity.this, getString(R.string.toast_history_selected), Toast.LENGTH_SHORT).show();
    }

    private void showProfileContent() {
        mapContainer.setVisibility(View.GONE);
        detailsPanel.setVisibility(View.GONE);
        roboflowImageViewNewUi.setVisibility(View.GONE);
        roboflowResultTextNewUi.setVisibility(View.GONE);
        Toast.makeText(MainActivity.this, getString(R.string.toast_profile_selected), Toast.LENGTH_SHORT).show();
    }


    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, getString(R.string.camera_permission_needed_for_feature), Toast.LENGTH_LONG).show();
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
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

    private void sendToRoboflow(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        // Anda BISA menambahkan parameter &confidence={threshold_persen} jika API Roboflow mendukungnya
        // Misalnya: float confidenceParam = currentConfidenceThreshold / 100.0f;
        // String inferenceApiUrl = ROBOFLOW_API_URL_PREFIX + ROBOFLOW_MODEL_ENDPOINT +
        //                          "?api_key=" + ROBOFLOW_API_KEY + "&confidence=" + currentConfidenceThreshold;
        // Namun, lebih umum memfilter di sisi klien.
        String inferenceApiUrl = ROBOFLOW_API_URL_PREFIX + ROBOFLOW_MODEL_ENDPOINT + "?api_key=" + ROBOFLOW_API_KEY;

        Log.d(TAG, "Sending image to: " + inferenceApiUrl);
        roboflowResultTextNewUi.setText(getString(R.string.text_processing_image)); // Update status

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, inferenceApiUrl,
                response -> {
                    try {
                        Log.i(TAG, "Roboflow Raw Response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        allPredictions = jsonResponse.optJSONArray("predictions");
                        if (allPredictions == null) {
                            allPredictions = new JSONArray(); // Pastikan tidak null
                        }
                        filterAndDisplayPredictions(); // Filter dan tampilkan

                    } catch (JSONException e) {
                        Log.e(TAG, "Roboflow JSON Parsing error: " + e.getMessage(), e);
                        roboflowResultTextNewUi.setText(getString(R.string.text_failed_parse_detection_result));
                        clearDamageDetails();
                    }
                },
                error -> {
                    // ... (Error handling sama seperti sebelumnya) ...
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

    private void filterAndDisplayPredictions() {
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
                double confidence = prediction.optDouble("confidence", 0.0) * 100; // Ubah ke persentase

                if (confidence >= currentConfidenceThreshold) {
                    filteredPredictions.add(prediction);
                    if (confidence > maxConfidence) { // Cari prediksi dengan confidence tertinggi
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
            // Pastikan kelas yang dideteksi adalah salah satu dari yang diharapkan
            if (className.equalsIgnoreCase("Minimum damage") ||
                    className.equalsIgnoreCase("moderate damage") || // Perhatikan case sensitivity
                    className.equalsIgnoreCase("major damage")) {
                textDamageValue.setText(className);
            } else {
                textDamageValue.setText(className); // Tampilkan apa adanya jika bukan salah satu dari 3 itu
                Log.w(TAG, "Detected class not one of expected: " + className);
            }
            textConfidenceValue.setText(String.format(Locale.getDefault(), "%.0f%%", maxConfidence));
            textRoadNameValue.setText("Dari Kamera"); // Atau dari GPS nanti

            // Tampilkan semua prediksi yang difilter di roboflowResultTextNewUi
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


    private void clearDamageDetails() {
        textRoadNameValue.setText(getString(R.string.placeholder_empty));
        textDamageValue.setText(getString(R.string.placeholder_empty));
        textConfidenceValue.setText(getString(R.string.placeholder_empty));
    }
}
