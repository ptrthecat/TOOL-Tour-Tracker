package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.lib.MyData;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // deklaracja zmiennych dla przycisku otwierającego mapę, fragmentu mapy i flagi
    private Button openMapButton;
    private SupportMapFragment mapFragment;
    private boolean mapOpened = false;

    private String[][][] danetab; // deklaracja zmiennej danetab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // inicjalizacja przycisku otwierającego mapę i fragmentu mapy
        openMapButton = findViewById(R.id.open_map_button);
        mapFragment = SupportMapFragment.newInstance();

        // ustawienie nasłuchiwacza dla przycisku otwierającego mapę
        openMapButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View v) {
                // sprawdzenie, czy mapa nie jest już otwarta
                if (!mapOpened) {
                    showLoadingDialog(); // wyświetlenie ekranu ładowania

                    // pobierania danych w tle za pomocą klasy AsyncTask
                    new AsyncTask<Void, Void, String[][][]>() {
                        @Override
                        protected String[][][] doInBackground(Void... voids) {
                            // utworzenie instancji klasy MyData
                            MyData myClass = new MyData();
                            try {
                                myClass.fillData(); // pobranie danych z klasy MyData przed inicjalizacją mapy
                                return myClass.getDanetab();
                            } catch (IOException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(String[][][] result) {
                            super.onPostExecute(result);
                            dismissLoadingDialog(); // zamknięcie ekranu ładowania

                            if (result != null) {
                                // przypisanie pobranych danych do zmiennej danetab jeśli pobrano poprawnie
                                danetab = result;

                                // aktualizacja mapy na podstawie pobranych danych
                                updateMapWithData();
                            } else {
                                // wystąpił błąd podczas pobierania danych
                                Toast.makeText(MainActivity.this, "Error while downloading data!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                }
            }
        });

        // nasłuchiwanie zmian na stosie fragmentów (np czy użytkownik cofnął w tył)
        getSupportFragmentManager().addOnBackStackChangedListener(new androidx.fragment.app.FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                // sprawdzenie czy stos fragmentów jest pusty, co oznacza zamknięcie mapy
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    mapOpened = false;
                    // wyświetlenie przycisku otwierającego mapę, gdy mapa jest zamknięta
                    openMapButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // metoda do aktualizacji mapy na podstawie danych pobranych z klasy MyData
    private void updateMapWithData() {
        // sprawdzenie czy dane zostały pomyślnie pobrane i czy są dostępne
        if (danetab != null && danetab.length > 0) {
            GoogleMapOptions options = new GoogleMapOptions(); // opcje dla mapy

            // ustawienie początkowych parametrów dla mapy
            options.mapType(GoogleMap.MAP_TYPE_NORMAL)  // typ mapy: standardowy
                    .compassEnabled(true)               // kompas
                    .rotateGesturesEnabled(true)        // gesty obracania
                    .zoomControlsEnabled(true)          // kontrola zoomu
                    .tiltGesturesEnabled(true);         // gesty pochylenia

            // Inicjalizujemy mapę
            SupportMapFragment mapFragment = SupportMapFragment.newInstance(options);

            // uzyskanie asynchronicznie referencji do obiektu GoogleMap
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    for(int k = 0; k < 30 - 1; k++) {
                        for (int i = 0; i < 10; i++) {
                            // pobranie współrzędnych geograficznych z danych mapy
                            double lat = Double.parseDouble(danetab[5][i][k]);
                            double lng = Double.parseDouble(danetab[6][i][k]);

                            // informacje do wyświetlenia nad pinezką
                            StringBuilder infoBuilder = new StringBuilder();
                            infoBuilder.append(danetab[1][i][k]);

                            int colorHueVal = 0;
                            // Pobierz wartość danetab[0][i][k]

                            String value = danetab[0][i][k];

                            String trimmedValue = value.substring(Math.max(0, value.length() - 4));

                            // wyróżnienie aktualnego roku
                            if(trimmedValue.equals("2024")){
                                colorHueVal = 300;
                            }

                            // utworzenie obiektu LatLng na podstawie współrzędnych geograficznych
                            LatLng location = new LatLng(lat, lng);

                            // opcje dla znaczników
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(location)
                                    .title(danetab[0][i][k])
                                    .snippet(infoBuilder.toString()) // dodanie informacji nad pinezką
                                    .icon(BitmapDescriptorFactory.defaultMarker(colorHueVal)); // ustawienie koloru na niebieski
                            googleMap.addMarker(markerOptions); // dodanie znacznika do mapy
                        }
                    }
                }
            });

            // Dodaj Fragment mapy do widoku
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main, mapFragment);  // zastąpienie istniejącego fragmentu mapy
            transaction.addToBackStack(null);      // dodanie zmiany do stosu cofania
            transaction.commit();

            // Ustaw flagę mapOpened na true
            mapOpened = true;
        } else {
            Toast.makeText(MainActivity.this, "No location data!", Toast.LENGTH_SHORT).show();
        }
    }


    private ProgressDialog loadingDialog; // zmienna przechowująca ekran ładowania

    // metoda do wyświetlania dialogu ładowania
    private void showLoadingDialog() {
        loadingDialog = new ProgressDialog(MainActivity.this); // inicjalizacja ekranu ładowania podczas głównej aktywności
        loadingDialog.setMessage("Downloading tour data...");
        loadingDialog.setCancelable(false); // użytkownik nie może zamknąć ekranu ładowania
        loadingDialog.setTitle("Please wait");
        loadingDialog.show(); // wyświetlenie ekranu ładowania
    }

    // metoda do zamykania dialogu ładowania
    private void dismissLoadingDialog() {
        // sprawdzenie czy dialog ładowania jest wyświetlany
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss(); // zamkniecie
        }
    }
}