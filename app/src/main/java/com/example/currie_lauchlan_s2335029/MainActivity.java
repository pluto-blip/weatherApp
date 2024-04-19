package com.example.currie_lauchlan_s2335029;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView weatherDisplay;
    private Button fetchButton;
    private Button backButton;

    private TextView locationDisplay;

    private Map<String, String> locationMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        weatherDisplay = findViewById(R.id.weatherDisplay);
        fetchButton = findViewById(R.id.fetchButton);
        fetchButton.setOnClickListener(this);

        initLocationMap();

        // Check if internet is connected
        if (!isInternetConnected()) {
            showNoInternetPopup();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == fetchButton) {
            showLocationSelectionDialog();
        } else if (view == backButton) {
            setContentView(R.layout.activity_home);
            fetchButton = findViewById(R.id.fetchButton);
            fetchButton.setOnClickListener(this);
            weatherDisplay = null;
            backButton = null;
        }
    }

    private void initLocationMap() {
        locationMap = new HashMap<>();
        locationMap.put("Glasgow", "2648579");
        locationMap.put("London", "2643743");
        locationMap.put("New York", "5128581");
        locationMap.put("Oman", "287286");
        locationMap.put("Mauritius", "934154");
        locationMap.put("Bangladesh", "1185241");
    }

    private void showLocationSelectionDialog() {
        final String[] locations = locationMap.keySet().toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Location");
        builder.setItems(locations, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String selectedLocation = locations[i];
                showObservationTypeSelectionDialog(selectedLocation);
            }
        });
        builder.show();
    }

    private void showObservationTypeSelectionDialog(final String selectedLocation) {
        final String[] observationTypes = {"3-day Forecast", "Latest Observation"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Observation Type");
        builder.setItems(observationTypes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String observationType = observationTypes[i];
                String locationCode = locationMap.get(selectedLocation);
                String url = observationType.equals("3-day Forecast") ?
                        "https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/" + locationCode :
                        "https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/" + locationCode;
                fetchWeatherData(url, selectedLocation);
            }
        });
        builder.show();
    }

    private void fetchWeatherData(String url, final String selectedLocation) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL weatherUrl = new URL(url);
                    URLConnection connection = weatherUrl.openConnection();
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    final String weatherInfo = parseWeatherData(response.toString());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setContentView(R.layout.activity_weather);
                            weatherDisplay = findViewById(R.id.weatherDisplay);
                            backButton = findViewById(R.id.backButton);
                            locationDisplay = findViewById(R.id.locationDisplay);
                            locationDisplay.setText(selectedLocation);
                            weatherDisplay.setText(weatherInfo);
                            backButton.setOnClickListener(MainActivity.this);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String parseWeatherData(String xmlData) {
        StringBuilder weatherInfo = new StringBuilder();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new java.io.StringReader(xmlData));

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("item")) {
                    weatherInfo.append(parseItem(parser));
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return weatherInfo.toString();
    }

    private String parseItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        StringBuilder itemInfo = new StringBuilder();
        int eventType = parser.next();

        while (!(eventType == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("item"))) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("title")) {
                String title = parser.nextText();
                int commaIndex = title.indexOf(",");
                if (commaIndex != -1) {
                    title = title.substring(0, commaIndex);
                }
                itemInfo.append(title.trim()).append("\n");
            } else if (eventType == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("description")) {
                String description = parser.nextText();
                String[] parts = description.split(", ");
                for (String part : parts) {
                    itemInfo.append(part).append("\n");
                }
                itemInfo.append("\n");
            }
            eventType = parser.next();
        }

        return itemInfo.toString();
    }

    private boolean isInternetConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
// internet popup thing, makes them unable to use app until wifi is on
    private void showNoInternetPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("No internet connection. Please connect to the internet to use this app.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        finish(); // Close app when user clicks ok prompt
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
