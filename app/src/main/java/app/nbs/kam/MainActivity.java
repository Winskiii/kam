package app.nbs.kam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    // UI Elements
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final LatLng defaultLocation = new LatLng(-6.2088, 106.8456);
    private EditText editTextSearch;
    private ImageButton buttonSearch;
    private TextView toolbarHelpButton, toolbarSignOutButton;
    private ImageView detailsBackButton;
    private TextView textRoadNameValue, textDamageValue, textConfidenceValue;
    private LinearLayout detailsPanel;
    private SeekBar seekbarConfidence;
    private TextView textConfidenceSliderValue;
    private ImageView roboflowImageViewNewUi;
    private TextView roboflowResultTextNewUi;
    private BottomNavigationView bottomNavigationView;
    private View mapFrameContainer;
    private CardView chatbotCard;
    private ProgressBar chatbotProgressBar;
    private TextView textChatbotResponse;

    // State Variables
    private int currentConfidenceThreshold = 70;
    private Bitmap currentPhoto;
    private JSONArray allPredictions = new JSONArray();

    // Roboflow API - Menggunakan URL lengkap yang Anda berikan
    private static final String ROBOFLOW_API_URL = "https://detect.roboflow.com/road-damage-fhdff/1?api_key=GzmSCfORrjN5uttBwYNf";

    // OpenAI API
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    // !!! PERINGATAN KEAMANAN: Jangan pernah menempatkan API key secara langsung di kode aplikasi produksi.
    // !!! Ganti dengan API Key Anda yang valid.
    private static final String OPENAI_API_KEY = "sk-proj-aclufF3BV3QMdrGjmphkGpO8Pyl1_0b5CAPIcVIWeGHbCg_uJyvxei2-_lrhyHRtnwNS6k_sVKjLqT3BlbkFJmV-mjdrGWKpDZjF-HBeLB2mXxKi-b2x95AZArJzpZume4jui2hh9kP0nil_2EojkQ4oI4WKtoA";

    // Launchers
    private ActivityResultLauncher<Intent> cameraActivityResultLauncher, galleryActivityResultLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher, requestGalleryPermissionLauncher;
    private ActivityResultLauncher<String[]> requestLocationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupActivityResultLaunchers();
        setupListeners();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        bottomNavigationView.setSelectedItemId(R.id.navigation_devices);
        showDevicesContent();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready.");
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        checkLocationPermissionAndEnableMyLocation();
    }

    private void initializeViews() {
        toolbarHelpButton = findViewById(R.id.toolbar_help_button);
        toolbarSignOutButton = findViewById(R.id.toolbar_sign_out_button);
        detailsBackButton = findViewById(R.id.details_back_button);
        textRoadNameValue = findViewById(R.id.text_road_name_value);
        textDamageValue = findViewById(R.id.text_damage_value);
        textConfidenceValue = findViewById(R.id.text_confidence_value);
        detailsPanel = findViewById(R.id.details_panel);
        seekbarConfidence = findViewById(R.id.seekbar_confidence);
        textConfidenceSliderValue = findViewById(R.id.text_confidence_slider_value);
        roboflowImageViewNewUi = findViewById(R.id.roboflow_image_view_new_ui);
        roboflowResultTextNewUi = findViewById(R.id.roboflow_result_text_new_ui);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        mapFrameContainer = findViewById(R.id.map_frame_container);
        editTextSearch = findViewById(R.id.edit_text_search);
        buttonSearch = findViewById(R.id.button_search);
        chatbotCard = findViewById(R.id.chatbot_card);
        chatbotProgressBar = findViewById(R.id.chatbot_progress_bar);
        textChatbotResponse = findViewById(R.id.text_chatbot_response);

        seekbarConfidence.setProgress(currentConfidenceThreshold);
        textConfidenceSliderValue.setText(String.format(Locale.getDefault(), getString(R.string.text_confidence_percentage), currentConfidenceThreshold));
    }

    private void setupActivityResultLaunchers() {
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) openCamera();
            else Toast.makeText(this, getString(R.string.toast_camera_permission_denied), Toast.LENGTH_SHORT).show();
        });
        cameraActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) processBitmap((Bitmap) extras.get("data"));
                else Toast.makeText(this, getString(R.string.toast_failed_get_image_data_from_camera), Toast.LENGTH_SHORT).show();
            }
        });
        requestGalleryPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) openGallery();
            else Toast.makeText(this, getString(R.string.toast_gallery_permission_denied), Toast.LENGTH_SHORT).show();
        });
        galleryActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    try {
                        Bitmap bitmap = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ?
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.getContentResolver(), imageUri)) :
                                MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        processBitmap(bitmap);
                    } catch (IOException e) { Log.e(TAG, "Error loading image from gallery", e); }
                }
            }
        });
        requestLocationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    Boolean fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (Boolean.TRUE.equals(fineLocationGranted) || Boolean.TRUE.equals(coarseLocationGranted)) enableMyLocation();
                    else Toast.makeText(this, getString(R.string.location_permission_needed), Toast.LENGTH_LONG).show();
                }
        );
    }

    private void setupListeners() {
        toolbarHelpButton.setOnClickListener(v -> Toast.makeText(this, getString(R.string.toast_help_clicked), Toast.LENGTH_SHORT).show());
        toolbarSignOutButton.setOnClickListener(v -> Toast.makeText(this, getString(R.string.toast_sign_out_clicked), Toast.LENGTH_SHORT).show());
        detailsBackButton.setOnClickListener(v -> showDevicesContent());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_devices) {
                showDevicesContent();
                return true;
            } else if (itemId == R.id.navigation_camera) {
                showImageSourceDialog();
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
                if (fromUser && allPredictions.length() > 0) filterAndDisplayPredictions();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { Log.d(TAG, "Confidence threshold set to: " + currentConfidenceThreshold + "%"); }
        });

        buttonSearch.setOnClickListener(v -> performSearch());
        editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void showImageSourceDialog() {
        showCameraContent();
        CharSequence[] options = {getString(R.string.option_camera), getString(R.string.option_gallery)};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_select_image_source))
                .setItems(options, (dialog, item) -> {
                    if (item == 0) checkCameraPermissionAndOpenCamera();
                    else if (item == 1) checkGalleryPermissionAndOpenGallery();
                })
                .show();
    }

    private void showDevicesContent() {
        mapFrameContainer.setVisibility(View.VISIBLE);
        detailsPanel.setVisibility(View.VISIBLE);
        roboflowImageViewNewUi.setVisibility(View.GONE);
        roboflowResultTextNewUi.setVisibility(View.GONE);
        chatbotCard.setVisibility(allPredictions.length() > 0 ? View.VISIBLE : View.GONE);
    }

    private void showCameraContent() {
        mapFrameContainer.setVisibility(View.GONE);
        detailsPanel.setVisibility(View.VISIBLE);
        roboflowImageViewNewUi.setVisibility(View.VISIBLE);
        roboflowResultTextNewUi.setVisibility(View.VISIBLE);
        roboflowResultTextNewUi.setText(getString(R.string.placeholder_detection_results));
        roboflowImageViewNewUi.setImageDrawable(null);
        chatbotCard.setVisibility(View.GONE);
        clearDamageDetails();
        allPredictions = new JSONArray();
    }

    private void showHistoryContent() {
        mapFrameContainer.setVisibility(View.GONE);
        detailsPanel.setVisibility(View.GONE);
        Toast.makeText(this, getString(R.string.toast_history_selected), Toast.LENGTH_SHORT).show();
    }

    private void showProfileContent() {
        mapFrameContainer.setVisibility(View.GONE);
        detailsPanel.setVisibility(View.GONE);
        Toast.makeText(this, getString(R.string.toast_profile_selected), Toast.LENGTH_SHORT).show();
    }

    private void processBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            currentPhoto = bitmap;
            roboflowImageViewNewUi.setImageBitmap(currentPhoto);
            sendToRoboflow(currentPhoto);
        } else {
            Toast.makeText(this, getString(R.string.toast_failed_get_image_from_camera), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera();
        else requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try { cameraActivityResultLauncher.launch(intent); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, getString(R.string.toast_no_camera_app), Toast.LENGTH_SHORT).show(); }
    }

    private void checkGalleryPermissionAndOpenGallery() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) openGallery();
        else requestGalleryPermissionLauncher.launch(permission);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try { galleryActivityResultLauncher.launch(intent); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Aplikasi galeri tidak ditemukan.", Toast.LENGTH_SHORT).show(); }
    }

    private void performSearch() {
        String searchString = editTextSearch.getText().toString().trim();
        if (!searchString.isEmpty()) {
            hideKeyboard();
            geocodeAddress(searchString);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) view = new View(this);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void geocodeAddress(String addressString) {
        if (mMap == null) return;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        new Thread(() -> {
            try {
                List<Address> addressList = geocoder.getFromLocationName(addressString, 1);
                runOnUiThread(() -> {
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(latLng).title(addressString));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                        textRoadNameValue.setText(address.getAddressLine(0));
                    } else Toast.makeText(MainActivity.this, getString(R.string.toast_address_not_found), Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) { Log.e(TAG, "Geocoding failed", e); }
        }).start();
    }

    private void checkLocationPermissionAndEnableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) enableMyLocation();
        else requestLocationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15f));
                else Log.w(TAG, "Last location is null.");
            });
        }
    }

    private void sendToRoboflow(Bitmap bitmap) {
        if (mMap != null) reverseGeocode(mMap.getCameraPosition().target);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        // --- PERBAIKAN DI SINI ---
        // Menggunakan konstanta ROBOFLOW_API_URL yang sudah lengkap secara langsung.
        String inferenceApiUrl = ROBOFLOW_API_URL;

        Log.d(TAG, "Sending image to: " + inferenceApiUrl);

        RequestQueue queue = Volley.newRequestQueue(this);
        roboflowResultTextNewUi.setText(getString(R.string.text_processing_image));
        StringRequest stringRequest = new StringRequest(Request.Method.POST, inferenceApiUrl,
                response -> {
                    try {
                        allPredictions = new JSONObject(response).optJSONArray("predictions");
                        if (allPredictions == null) allPredictions = new JSONArray();
                        filterAndDisplayPredictions();
                    } catch (JSONException e) {
                        Log.e(TAG, "Roboflow JSON Parsing error", e);
                        roboflowResultTextNewUi.setText(getString(R.string.text_failed_parse_detection_result));
                        clearDamageDetails();
                    }
                },
                error -> {
                    Log.e(TAG, "VolleyError", error);
                    roboflowResultTextNewUi.setText(getString(R.string.text_failed_send_image_to_roboflow));
                    clearDamageDetails();
                }
        ) {
            @Override public String getBodyContentType() { return "application/x-www-form-urlencoded"; }
            @Override public byte[] getBody() throws AuthFailureError { return encodedImage.getBytes(); }
        };
        queue.add(stringRequest);
    }

    private void reverseGeocode(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) textRoadNameValue.setText(addresses.get(0).getAddressLine(0));
                    else textRoadNameValue.setText("Jalan tidak diketahui");
                });
            } catch (IOException e) {
                Log.e(TAG, "Reverse geocoding failed", e);
                runOnUiThread(() -> textRoadNameValue.setText("Gagal mendapatkan nama jalan"));
            }
        }).start();
    }

    private void filterAndDisplayPredictions() {
        if (allPredictions.length() == 0) {
            roboflowResultTextNewUi.setText(getString(R.string.text_no_damage_detected));
            chatbotCard.setVisibility(View.GONE);
            clearDamageDetails();
            return;
        }
        JSONObject bestPrediction = null;
        double maxConfidence = -1.0;
        try {
            for (int i = 0; i < allPredictions.length(); i++) {
                JSONObject prediction = allPredictions.getJSONObject(i);
                double confidence = prediction.optDouble("confidence") * 100;
                if (confidence >= currentConfidenceThreshold && confidence > maxConfidence) {
                    maxConfidence = confidence;
                    bestPrediction = prediction;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error filtering predictions", e);
            clearDamageDetails();
            return;
        }

        if (bestPrediction != null) {
            String damageClass = bestPrediction.optString("class", "N/A");
            String confidenceText = String.format(Locale.getDefault(), "%.0f%%", maxConfidence);
            String location = textRoadNameValue.getText().toString();
            textDamageValue.setText(damageClass);
            textConfidenceValue.setText(confidenceText);
            generateAndSendOpenAIPrompt(damageClass, confidenceText, location);
        } else {
            roboflowResultTextNewUi.setText(getString(R.string.text_no_damage_detected) + " (di atas " + currentConfidenceThreshold + "%)");
            chatbotCard.setVisibility(View.GONE);
            clearDamageDetails();
        }
    }

    private void generateAndSendOpenAIPrompt(String damageClass, String confidence, String location) {
        if (location.equals(getString(R.string.placeholder_empty)) || location.isEmpty()) {
            location = "lokasi saat ini (detail tidak tersedia)";
        }
        String prompt = String.format(Locale.getDefault(), getString(R.string.openai_prompt_template),
                damageClass, confidence, location);
        Log.d(TAG, "Generated OpenAI Prompt: " + prompt);
        getOpenAIChatResponse(prompt);
    }

    private void getOpenAIChatResponse(String prompt) {
        chatbotCard.setVisibility(View.VISIBLE);
        chatbotProgressBar.setVisibility(View.VISIBLE);
        textChatbotResponse.setText(getString(R.string.chatbot_thinking));

        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "gpt-3.5-turbo");
            JSONArray messagesArray = new JSONArray();
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a helpful assistant for a road damage reporting app named Roady.");
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messagesArray.put(systemMessage);
            messagesArray.put(userMessage);
            requestBody.put("messages", messagesArray);
            requestBody.put("max_tokens", 150);

        } catch (JSONException e) {
            Log.e(TAG, "Failed to create OpenAI request body", e);
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, OPENAI_API_URL, requestBody,
                response -> {
                    chatbotProgressBar.setVisibility(View.GONE);
                    try {
                        String botResponse = response.getJSONArray("choices").getJSONObject(0)
                                .getJSONObject("message").getString("content");
                        textChatbotResponse.setText(botResponse.trim());
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse OpenAI response", e);
                        textChatbotResponse.setText(getString(R.string.chatbot_error));
                    }
                },
                error -> {
                    chatbotProgressBar.setVisibility(View.GONE);
                    Log.e(TAG, "OpenAI API Error: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "OpenAI Error Body: " + new String(error.networkResponse.data, StandardCharsets.UTF_8));
                    }
                    textChatbotResponse.setText(getString(R.string.chatbot_error));
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + OPENAI_API_KEY);
                return headers;
            }
        };
        queue.add(jsonObjectRequest);
    }

    private void clearDamageDetails() {
        textRoadNameValue.setText(getString(R.string.placeholder_empty));
        textDamageValue.setText(getString(R.string.placeholder_empty));
        textConfidenceValue.setText(getString(R.string.placeholder_empty));
        chatbotCard.setVisibility(View.GONE);
    }
}
