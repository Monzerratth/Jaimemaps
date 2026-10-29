package com.example.jaimemaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_REQUEST_CODE = 101;
    private static final String STATE_UNLOCKED = "state_unlocked";
    /** Tiempo durante el cual una autenticación reciente autoriza acciones sensibles. */
    private static final long AUTH_VALIDITY_MS = 30_000L;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ImageView ivPhoto;
    private View rootLayout;
    private MarkerDbHelper db;

    private boolean unlocked = false;   // ¿el usuario ya se autenticó al abrir la app?
    private long lastAuthMillis = 0L;   // momento de la última autenticación (0 = ninguna)

    private final List<Marker> markers = new ArrayList<>();

    // Captura una imagen con la cámara y la muestra en pantalla
    private final ActivityResultLauncher<Void> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    ivPhoto.setImageBitmap(bitmap);
                    ivPhoto.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        ivPhoto = findViewById(R.id.ivPhoto);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = new MarkerDbHelper(this);

        // Cargar el mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Botones
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> checkLocationPermission());
        findViewById(R.id.btnClearMarkers).setOnClickListener(v -> clearMarkers());
        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> launchCamera());
        ivPhoto.setOnClickListener(v -> ivPhoto.setVisibility(View.GONE)); // tocar la foto la oculta

        // AUTENTICACIÓN: la app queda oculta hasta que el usuario se autentique.
        unlocked = savedInstanceState != null && savedInstanceState.getBoolean(STATE_UNLOCKED, false);
        if (!unlocked) {
            rootLayout.setVisibility(View.INVISIBLE);
            unlockApp();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_UNLOCKED, unlocked);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Tocar el mapa = pedir un nombre y añadir un marcador
        mMap.setOnMapClickListener(this::promptMarkerTitle);

        loadSavedMarkers();

        if (unlocked) {
            checkLocationPermission();
        }
    }

    // ---------- Autenticación y autorización ----------

    /** Puerta de entrada: sin autenticación correcta la app se cierra. */
    private void unlockApp() {
        AuthHelper.authenticate(this, new AuthHelper.Callback() {
            @Override
            public void onSuccess() {
                unlocked = true;
                lastAuthMillis = SystemClock.elapsedRealtime();
                rootLayout.setVisibility(View.VISIBLE);
                if (mMap != null) {
                    checkLocationPermission();
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * AUTORIZACIÓN: las acciones sensibles (borrar datos) exigen una autenticación
     * reciente. Si pasaron más de 30 s se vuelve a pedir.
     */
    private void requireRecentAuth(Runnable sensitiveAction) {
        boolean recent = lastAuthMillis != 0
                && SystemClock.elapsedRealtime() - lastAuthMillis < AUTH_VALIDITY_MS;
        if (recent) {
            sensitiveAction.run();
            return;
        }
        AuthHelper.authenticate(this, new AuthHelper.Callback() {
            @Override
            public void onSuccess() {
                lastAuthMillis = SystemClock.elapsedRealtime();
                sensitiveAction.run();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(MainActivity.this, R.string.action_not_authorized,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------- Permisos y ubicación ----------

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (mMap == null) return; // el mapa aún no está listo
        mMap.setMyLocationEnabled(true); // punto azul del GPS
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15));
                mMap.addMarker(new MarkerOptions()
                        .position(current)
                        .title("Ubicación actual")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            } else {
                Toast.makeText(this, "No hay ubicación disponible. Activa el GPS.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------- Marcadores (con validación de entrada y SQL seguro) ----------

    /** Pide el nombre del marcador al usuario. */
    private void promptMarkerTitle(LatLng latLng) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.marker_name_hint);
        input.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(InputValidator.MAX_TITLE_LENGTH)});

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.marker_dialog_title)
                .setView(container)
                .setPositiveButton(android.R.string.ok,
                        (d, w) -> saveMarker(latLng, input.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void saveMarker(LatLng latLng, String rawTitle) {
        // 1) Validar y sanitizar la entrada (lista blanca)
        String title = InputValidator.sanitizeTitle(rawTitle);
        if (title == null) {
            Toast.makeText(this, R.string.marker_invalid, Toast.LENGTH_LONG).show();
            return;
        }
        // 2) Guardar con consulta parametrizada (sin concatenar SQL)
        long id = db.insertMarker(title, latLng.latitude, latLng.longitude);
        if (id == -1) {
            Toast.makeText(this, R.string.marker_save_error, Toast.LENGTH_SHORT).show();
            return;
        }
        addMarkerToMap(title, latLng, true);
    }

    private void addMarkerToMap(String title, LatLng latLng, boolean showInfo) {
        String coords = String.format(Locale.US, "Lat: %.5f, Lng: %.5f",
                latLng.latitude, latLng.longitude);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(coords));
        if (marker != null) {
            markers.add(marker);
            if (showInfo) marker.showInfoWindow();
        }
    }

    /** Restaura los marcadores guardados en la base de datos. */
    private void loadSavedMarkers() {
        for (MarkerDbHelper.SavedMarker m : db.getAllMarkers()) {
            addMarkerToMap(m.title, new LatLng(m.lat, m.lng), false);
        }
    }

    private void clearMarkers() {
        // Acción sensible: requiere autenticación reciente
        requireRecentAuth(() -> {
            for (Marker marker : markers) {
                marker.remove();
            }
            markers.clear();
            db.deleteAllMarkers();
            Toast.makeText(this, "Marcadores eliminados", Toast.LENGTH_SHORT).show();
        });
    }

    // ---------- Cámara ----------

    private void launchCamera() {
        try {
            takePicture.launch(null);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }
}
