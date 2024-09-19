package com.example.grabi;
import android.Manifest;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import java.lang.Math;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import android.widget.RadioGroup;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.RadioButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private CardView myCardView;
    private CardView myCardView1;
    private CardView myCardView2;
    private RadioButton mild;
    private RadioButton moderate;
    private RadioButton severe;
    private CardView myCardView3;
    private RequestQueue requestQueue;
    private String apiKey = "YOUR API_KEY";

    private static final String TAG = "MainActivity";
    private LocationManager locationManager;
    private LocationListener locationListener;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static final int REQUEST_EXACT_ALARM_PERMISSION = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 3;
    private static final int REQUEST_USAGE_STATS = 100;
    private UsageStatsManager mUsageStatsManager;
    private TextView mScreenTimeTextView;
    private TextView loc;
    private TextView weather;
    private String place;
    private double latitude;
    private  double longitude;
    private long screenTime;
    private Button selectedButton;
    private int selectedId;
    private Button showCardButton;
    private String lat;
    private String lon;
    private double temperature;
    private int humidity;
    private int hour;
    private int dt;
    private String placeName;
    private TextView dryness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myCardView = findViewById(R.id.myCardView);
        myCardView1 = findViewById(R.id.myCardView1);
        weather=findViewById(R.id.weather);
        dryness=findViewById(R.id.dryness);
        myCardView2 = findViewById(R.id.myCardView2);
        myCardView3 = findViewById(R.id.myCardView3);
        showCardButton = findViewById(R.id.showCardButton);
        //  hidden first
        myCardView.setAlpha(0f);
        myCardView1.setAlpha(0f);
        myCardView2.setAlpha(0f);
        myCardView3.setAlpha(0f); // Set transparency to 1 (completely opaque)

        requestQueue = Volley.newRequestQueue(this);
        loc = findViewById(R.id.textView);
        RadioGroup radioGroup=findViewById(R.id.radio_group);
        RadioButton mild=findViewById(R.id.radio1);
        RadioButton moderate=findViewById(R.id.radio2);
        RadioButton severe=findViewById(R.id.radio3);
        mScreenTimeTextView = findViewById(R.id.screen_time_text_view);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        // Request POST_NOTIFICATIONS permission if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS,Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_NOTIFICATION_PERMISSION);
            }
            // Request location permissions if not already granted
            //   if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //     ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            // }
        }


        if (!hasUsageStatsPermission(this)) {
            requestUsageStatsPermission();
        }

        // Check and request SCHEDULE_EXACT_ALARM permission if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivityForResult(intent, REQUEST_EXACT_ALARM_PERMISSION);
            }
        }



        AlarmHelper.setDailyAlarms(MainActivity.this); // Set alarms

        showCardButton = findViewById(R.id.showCardButton);
        showCardButton.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if(selectedId!=-1) {
                selectedButton = (RadioButton) findViewById(selectedId);
                dryness.setText(selectedButton.getText().toString());
            }
            else {
                Toast.makeText(this,"You have not selected any option for eye dryness question",Toast.LENGTH_LONG).show();
            }
            myCardView.setAlpha(1f);
            myCardView1.setAlpha(1f);
            myCardView2.setAlpha(1f);
            myCardView3.setAlpha(1f);
            // Check location permission again
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted. Fetching location.");

                fetchLocation();
                calculateScreenTime();


            }
            else {
                Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchLocation() {
        try {

            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                latitude = lastLocation.getLatitude();
                longitude = lastLocation.getLongitude();
                fetchWeatherData(latitude, longitude);
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        placeName = addresses.get(0).getAddressLine(0);
                        loc.setText("Place name: " + placeName + "\n" + "Latitude: " + latitude + "\n" + "Longitude: " + longitude);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoder failed", e);
                }
            } else {
                Log.e(TAG, "Last known location is null.");
                Toast.makeText(this, "Unable to fetch location. Please check GPS settings.", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
        }
    }

    private void calculateScreenTime() {
        if (mUsageStatsManager == null) {
            Toast.makeText(this, "UsageStatsManager is null. Cannot retrieve usage stats.", Toast.LENGTH_LONG).show();
            return;
        }
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        dt=calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(Calendar.DAY_OF_YEAR, 0);
        hour=calendar.get(Calendar.HOUR_OF_DAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0); // Get usage stats
        long startTime = calendar.getTimeInMillis();
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(startTime, endTime);
        screenTime = getScreenTime(usageEvents);
        mScreenTimeTextView.setText(formatScreenTime(screenTime));
    }

    private long getScreenTime(UsageEvents usageEvents) {
        long screenTime = 0;
        long lastEventTime = 0;
        UsageEvents.Event event = new UsageEvents.Event();

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);

            if (event.getEventType() == UsageEvents.Event.SCREEN_INTERACTIVE) {
                lastEventTime = event.getTimeStamp();
            } else if (event.getEventType() == UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
                if (lastEventTime > 0) {
                    screenTime += event.getTimeStamp() - lastEventTime;
                    lastEventTime = 0;
                }
            }
        }

        if (lastEventTime > 0) {
            screenTime += event.getTimeStamp() - lastEventTime;
        }

        return screenTime;
    }

    private String formatScreenTime(long screenTimeMillis) {
        long hours = (screenTimeMillis / (1000 * 60 * 60)) % 24;
        long minutes = (screenTimeMillis / (1000 * 60)) % 60;
        long seconds = (screenTimeMillis / 1000) % 60;
        return hours + " : " + minutes + " : " + seconds;
    }

    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestUsageStatsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Please grant usage access permission", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Usage access permission required!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EXACT_ALARM_PERMISSION) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Exact alarm permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(this, "Exact alarm permission not granted.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission not granted.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted.");
            } else {
                Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void fetchWeatherData(double latitude, double longitude) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + apiKey;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Get the "main" object from the JSON response
                            JSONObject main = response.getJSONObject("main");

                            // Extract temperature and humidity from the "main" object
                            double temperature = main.getDouble("temp") - 273.15; // Convert from Kelvin to Celsius
                            humidity = main.getInt("humidity");

                            // Update the weather TextView with temperature and humidity
                            weather.setText("Temperature : " + Math.round(temperature) + "C" + "\n" + "Humidity : " + humidity);

                            // Format the date and time
                            SimpleDateFormat sdf = new SimpleDateFormat("'Date:'dd-MM-yyyy ;'and Time :'HH:mm:ss ;");
                            String ts = sdf.format(new Date());

                            if (selectedId != -1) {
                                writeToFile("" + ts + " ScreenTime: " + formatScreenTime(screenTime) + ";" + " Dryness: " + selectedButton.getText().toString() + ";" + " Temperature : " + Math.round(temperature) + ";" + "Humidity : " + humidity + ";" + " Place Name : " + placeName + ";" + " Latitude : " + latitude + ";" + " Longitude : " + longitude, "dry_data.txt");
                                writeToFile("", "dry_data.txt");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error parsing weather data", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Weather request error: " + error.getMessage());
                        Toast.makeText(MainActivity.this, "Error fetching weather data", Toast.LENGTH_SHORT).show();
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private synchronized void writeToFile(String data, String filename) {
        File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDirectory, filename);

        try {
            FileOutputStream fos;
            if (file.exists()) {
                // If file already exists, append data to it
                fos = new FileOutputStream(file, true);
                // Append a new line before adding new data to keep it separated from previous content
                fos.write("\n".getBytes());
            } else {
                // If file doesn't exist, create a new file
                fos = new FileOutputStream(file);
            }

            fos.write(data.getBytes());
            fos.close();
            Toast.makeText(this, "Data written to file", Toast.LENGTH_SHORT).show();
            String Symptoms=selectedButton.getText().toString();
      if((Symptoms.equals("Moderate") && screenTime>=21600000)||(Symptoms.equals("Severe")&&screenTime>=
                 21600000)||(Symptoms.equals("Moderate") && humidity<45)||(Symptoms.equals("Severe") && humidity<45)||(hour==7 && dt==1))
         {
             Intent testmode=new Intent(this,Testmode.class);
             startActivity(testmode);
         }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
