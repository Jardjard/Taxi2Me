package com.haldane.katherine.kh_jl_taxi2me;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class TaxiMap extends FragmentActivity {
    private static final String ACTION_STRING_ACTIVITY = "ToActivity";
    private static final String ACTION_STRING_SERVICE = "ToService";
    private Handler mHandler;
    private String paymentType = "";
    private double taxiLat = 0.0;
    private double taxiLong = 0.0;
    private double myLat;
    private double myLong;
    private String price = "";
    private boolean paymentRequested = false;

    Intent iPayment;
    Marker myLocMarker;
    Marker taxiLocMarker;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taxi_map);
        setUpMapIfNeeded();
        //Set up the spinner
        final String[] creditCardType = {"Visa","Mastercard","American Express","Discover", "JCB"};

        Spinner cardType = (Spinner) findViewById(R.id.spinnerCardType);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, creditCardType);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        cardType.setAdapter(dataAdapter);

        //Set up the icons (arrow, calendar)
        Typeface fontFamily = Typeface.createFromAsset(getAssets(), "fonts/fontawesome-webfont.ttf");
        TextView arrow = (TextView) findViewById(R.id.arrow);
        arrow.setTypeface(fontFamily);
        arrow.setText("\uf063");

        TextView calendar = (TextView) findViewById(R.id.calendar);
        calendar.setTypeface(fontFamily);
        calendar.setText("\uf073");

        //STEP2: register the receiver
        if (activityReceiver != null) {
            //Create an intent filter to listen to the broadcast sent with the action "ACTION_STRING_ACTIVITY"
            IntentFilter intentFilter = new IntentFilter(ACTION_STRING_ACTIVITY);
            //Map the intent filter to the receiver
            registerReceiver(activityReceiver, intentFilter);
        }
        //Start the service on launching the application
        startService(new Intent(this,Taxi2MeService.class));

        setUpMapIfNeeded();

        updateTimer();
    }

    public void updateTimer()
    {
        mHandler = new Handler();
        // thread requests an update from the service on a timer
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (!paymentRequested) {
                    try {
                        Thread.sleep(3000);
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                // get the service to ask for an update
                                GPSService mGPSService = new GPSService(getBaseContext());
                                mGPSService.getLocation();
                                if (mGPSService.isLocationAvailable == false) {
                                    Toast.makeText(getBaseContext(), "Your location is not available, please turn on GPS!.", Toast.LENGTH_SHORT).show();
                                    mGPSService.closeGPS();
                                    return;
                                } else {
                                    try {
                                        // Getting location co-ordinates
                                        double latitude = mGPSService.getLatitude();
                                        double longitude = mGPSService.getLongitude();
                                        mGPSService.closeGPS();
                                        Intent i = new Intent();
                                        i.setAction(ACTION_STRING_SERVICE);
                                        i.putExtra("ActionFlag", "getUpdate");
                                        i.putExtra("clientLatitude", latitude);
                                        i.putExtra("clientLongitude", longitude);
                                        i.putExtra("PaymentType", paymentType);
                                        Log.d("TaxiMapActivity", "Sending broadcast to service");
                                        sendBroadcast(i);

                                        if (taxiLat != 0.0 && taxiLong != 0.0) {

                                            // update your location
                                            //mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("You Are Here!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                                            animateMarker(myLocMarker, new LatLng(latitude, longitude), false);

                                            // update cab location
                                            if (taxiLocMarker == null) {
                                                taxiLocMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(taxiLat, taxiLong)).title("Your Cab!"));
                                                TextView mapNotification = (TextView) findViewById(R.id.mapNotification);
                                                mapNotification.setText("Your taxi has been dispatched and is on the way!");
                                            } else {
                                                animateMarker(taxiLocMarker, new LatLng(taxiLat, taxiLong), false);
                                            }

                                            //Lat long of the persons current location
                                            LatLng latlng1 = new LatLng(latitude < taxiLat ? latitude : taxiLat, longitude < taxiLong ? longitude : taxiLong);
                                            LatLng latlng2 = new LatLng(latitude < taxiLat ? taxiLat : latitude, longitude < taxiLong ? taxiLong : longitude);

                                            LatLngBounds latlngbound = new LatLngBounds(latlng1, latlng2);

                                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latlngbound, 300));
                                        } else if (taxiLocMarker != null) {
                                            // taxi has arrived
                                            animateMarker(taxiLocMarker, new LatLng(latitude, longitude), false);
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
                                            if (!price.contentEquals("") && !paymentRequested) {
                                                paymentRequested = true;
                                                if (paymentType.contentEquals("CreditPhone"))
                                                    startActivity(iPayment);
                                                else {
                                                    Toast.makeText(getApplicationContext(), "Thanks for riding with us!", Toast.LENGTH_LONG).show();
                                                    Intent iClear = getBaseContext().getPackageManager()
                                                            .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                                                    iClear.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                    startActivity(iClear);
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        // TODO: handle exception
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 2000;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    //Methods that are needed to talk to the service
    //STEP1: Create a broadcast receiver
    private BroadcastReceiver activityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            try {
                paymentType = bundle.getString("PaymentType", paymentType);

                taxiLat = Double.parseDouble(bundle.getString("TaxiLatitude", "0.0"));
                taxiLong = Double.parseDouble(bundle.getString("TaxiLongitude", "0.0"));

                if (price.equals("") && bundle.getString("MessageType").contentEquals("RequestPayment")) {
                    price = bundle.getString("Price");
                    iPayment = new Intent(TaxiMap.this, PaymentReceipt.class);
                    iPayment.putExtra("Name", bundle.getString("Name"));
                    iPayment.putExtra("CreditNumber", bundle.getString("CreditNumber"));
                    iPayment.putExtra("CreditType", bundle.getString("CreditType"));
                    iPayment.putExtra("Price", price);
                }
            }
            catch (NullPointerException e) {
                //lul null ptr
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
//        if (mMap == null) {
//            // Try to obtain the map from the SupportMapFragment.
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
//            // Check if we were successful in obtaining the map.
//            if (mMap != null) {
        setUpMap();
        //          }
//        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        GPSService mGPSService = new GPSService(getBaseContext());
        try {
            //Get the current location of the user
            mGPSService.getLocation();

            if (mGPSService.isLocationAvailable == false) {

                // Here you can ask the user to try again, using return; for that
                Toast.makeText(getBaseContext(), "Your location is not available, please try again.", Toast.LENGTH_SHORT).show();
                return;

                // Or you can continue without getting the location, remove the return; above and uncomment the line given below
                // address = "Location not available";
            } else {
                // Getting location co-ordinates
                myLat = mGPSService.getLatitude();
                myLong = mGPSService.getLongitude();
                LatLng myLoc = new LatLng(myLat, myLong);

                myLocMarker = mMap.addMarker(new MarkerOptions().position(myLoc).title("You Are Here!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))); //.showInfoWindow();
                myLocMarker.showInfoWindow();

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLoc, 15));
            }
            // make sure you close the gps after using it. Save user's battery power
            mGPSService.closeGPS();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //response = "IOException: " + e.toString();
            mGPSService.closeGPS();
        }
    }


    public void onButtonClick(View view)
    {
        EditText name= (EditText) findViewById(R.id.editTextName);
        EditText ccn= (EditText) findViewById(R.id.editTextCreditCardNumber);
        EditText ccv= (EditText) findViewById(R.id.editTextCcv);
        Spinner cardType = (Spinner) findViewById(R.id.spinnerCardType);
        RelativeLayout datePicker = (RelativeLayout) findViewById(R.id.dateLayout);
        RelativeLayout dateButtons = (RelativeLayout) findViewById(R.id.dateButtons);
        EditText date = (EditText) findViewById(R.id.editTextDate);
        Button buttonUpdateInfo = (Button) findViewById(R.id.buttonUpdateInfo);
        TextView mapNotification = (TextView) findViewById(R.id.mapNotification);
        LinearLayout creditInfoLayout = (LinearLayout) findViewById(R.id.creditInfoLayout);
        LinearLayout fragmentLayout = (LinearLayout) findViewById(R.id.fragmentLayout);
        switch(view.getId()) {
            case R.id.buttonMap:
                fragmentLayout.setVisibility(View.VISIBLE);
                creditInfoLayout.setVisibility(View.GONE);
                mapNotification.setVisibility(View.VISIBLE);
                break;
            case R.id.buttonCancelTaxi:
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:5198726992"));
                startActivity(callIntent);
                break;
            case R.id.buttonCreditInfo:
                fragmentLayout.setVisibility(View.GONE);
                mapNotification.setVisibility(View.GONE);
                creditInfoLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.buttonUpdateInfo:
                //Validate
                //Validate Name
                if(name.getText().toString().trim().length() == 0)
                {
                    Toast.makeText(getApplicationContext(), "Please Enter a Name!", Toast.LENGTH_SHORT).show();
                }
                //Validate Credit Card Number
                else if (ccn.getText().length() != 16)
                {
                    Toast.makeText(getApplicationContext(), "Invalid Credit Card Number! Must be 16 digits!", Toast.LENGTH_SHORT).show();
                }
                //Validate ccv
                else if (ccv.getText().length() != 3)
                {
                    Toast.makeText(getApplicationContext(), "Invalid CCV! Must be 3 digits!", Toast.LENGTH_SHORT).show();
                }
                //Validate Date
                else if(date.getText().toString().trim().length() == 0)
                {
                    Toast.makeText(getApplicationContext(), "Please Enter a Date!", Toast.LENGTH_SHORT).show();
                }
                else {
                    //Send the new credit information to the service
                    Intent serverI = new Intent();
                    serverI.setAction(ACTION_STRING_SERVICE);
                    serverI.putExtra("ActionFlag", "continueStartupFlag");
                    serverI.putExtra("Name", name.getText().toString());
                    serverI.putExtra("CreditType", cardType.getSelectedItem().toString());
                    serverI.putExtra("CreditNumber", ccn.getText().toString());
                    serverI.putExtra("CCV", ccv.getText().toString());
                    serverI.putExtra("ExpireDate", date.getText().toString());
                    Log.d("SampleActivity", "Sending broadcast to service - update credit information button");
                    sendBroadcast(serverI);

                    Toast.makeText(getBaseContext(), "Credit information has been updated!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.buttonSet:
                DatePicker datePicked = (DatePicker) findViewById(R.id.datePickerFrag);
                EditText setDate= (EditText) findViewById(R.id.editTextDate);
                setDate.setText(datePicked.getYear() + "/" + datePicked.getMonth());
                datePicker.setVisibility(View.GONE);
                dateButtons.setVisibility(View.GONE);
                date.setVisibility(View.VISIBLE);
                buttonUpdateInfo.setVisibility(View.VISIBLE);
                break;
            case R.id.buttonCancel:
                datePicker.setVisibility(View.GONE);
                dateButtons.setVisibility(View.GONE);
                date.setVisibility(View.VISIBLE);
                buttonUpdateInfo.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void onClick(View view) {
        RelativeLayout datePicker = (RelativeLayout) findViewById(R.id.dateLayout);
        RelativeLayout dateButtons = (RelativeLayout) findViewById(R.id.dateButtons);
        EditText date = (EditText) findViewById(R.id.editTextDate);
        Button buttonUpdateInfo = (Button) findViewById(R.id.buttonUpdateInfo);
        switch (view.getId()) {
            case R.id.editTextDate:
                //open new calendar fragment
                datePicker.setVisibility(View.VISIBLE);
                dateButtons.setVisibility(View.VISIBLE);
                date.setVisibility(View.GONE);
                buttonUpdateInfo.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("TaxiMap", "onDestroy");
        //STEP3: Unregister the receiver
        unregisterReceiver(activityReceiver);
    }
}
