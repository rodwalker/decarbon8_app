package com.example.decarbon8_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    TextView countryCode; // Local electricity grid - DE,GB,NL,....
    TextView maximumDelay; // hours user willing to wait for appliance cycle to start
    SeekBar maxDelaySeekBar;
    TextView suggestedDelay; // in hours

    Button requestDelay; // click to send info to RESTful API, and get optimum delay back

    RequestQueue requestQueue; // This is our requests queue to process our HTTP requests.
    String baseUrl = "https://helloworld-rod.appspot.com/";
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        countryCode = (TextView) findViewById(R.id.countryCode);
        maximumDelay = (TextView) findViewById(R.id.maximumDelay);
        maxDelaySeekBar = (SeekBar) findViewById(R.id.maxDelaySeekBar);
        suggestedDelay = (TextView) findViewById(R.id.suggestedDelay);
        requestDelay = (Button) findViewById(R.id.getDelay);

        requestQueue = Volley.newRequestQueue(this); // This setups up a new request queue which we will need to make HTTP requests
        setupSharedPreferences();

    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        // inialize from preferences
        onSharedPreferenceChanged(sharedPreferences, "country"); // simulate preference change to trigger initialization
        onSharedPreferenceChanged(sharedPreferences, "maxDelay"); // simulate preference change to trigger initialization

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("country")) {
            countryCode.setText(sharedPreferences.getString("country","Not set"));
        }
        if (key.equals("maxDelay")) {
            String maxDelayStr = sharedPreferences.getString("maxDelay","12");
            Integer maxDelay = Integer.valueOf(maxDelayStr);

            //maximumDelay.setText(sharedPreferences.getString("maxDelay","12"));
            maximumDelay.setText(maxDelay.toString());
            maxDelaySeekBar.setProgress(maxDelay);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

//https://www.londonappdeveloper.com/consuming-a-json-rest-api-in-android/

    private void getDelay(){
//        String maxDelay = maximumDelay.getText().toString();
        Integer maxDelay = maxDelaySeekBar.getProgress();
        String cCode = countryCode.getText().toString();
        url = baseUrl + "delay?maxDelay="+maxDelay+"&country="+cCode;

        suggestedDelay.setText(url);
        // Next, we create a new JsonArrayRequest. This will use Volley to make a HTTP request
        // that expects a JSON Array Response.
        // To fully understand this, I'd recommend readng the office docs: https://developer.android.com/training/volley/index.html
        JsonObjectRequest arrReq = new JsonObjectRequest(Request.Method.GET, url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                                try {
                                    // For each repo, add a new line to our repo list.
                                    JSONObject jsonObj = response;
                                    String delay = jsonObj.get("delay").toString();
                                    Double min = (Double) jsonObj.get("min");
                                    Double ref = (Double) jsonObj.get("ref");
                                    Boolean warn = (Boolean) jsonObj.get("warn");
                                    suggestedDelay.setText("Set delay for "+delay+" hrs to emit "+min+" gCO2 rather than "+ref);
                                } catch (JSONException e) {
                                    // If there is an error then output this to the logs.
                                    Log.e("Volley", "Invalid JSON Object.");
                                }

                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // If there a HTTP error then add a note to our repo list.
                        suggestedDelay.setText("Error: "+url);
                        Log.e("Volley", error.toString());
                    }
                }
        );
        // Add the request we just defined to our request queue.
        // The request queue will automatically handle the request as soon as it can.
 //10000 is the time in milliseconds adn is equal to 10 sec
    arrReq.setRetryPolicy(new DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

    requestQueue.add(arrReq);

    }
    public void getDelayClicked(View v) {
        getDelay();
        //suggestedDelay.setText("Poo");
    }
}
