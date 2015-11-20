package com.haldane.katherine.kh_jl_taxi2me;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;


public class Splash extends Activity {
    //Strings to register to create intent filter for registering the recivers
    private static final String ACTION_STRING_SERVICE = "ToService";
    private static final String ACTION_STRING_ACTIVITY = "ToActivity";

    //Methods that are needed to talk to the service
    //STEP1: Create a broadcast receiver
    private BroadcastReceiver activityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Set the String here to what has been send back
            if(intent.getExtras() != null) {
                Bundle bundle = intent.getExtras();
                String startupFlag = bundle.getString("StartupFlag");

                //Send the message to the Java server\
                if (startupFlag != null) {
                    if(startupFlag.equals("0")) {
                        Log.d("Activity", "Sending the intent to RegisterPreferences - startupFlag of 0");
                        Intent i = new Intent(Splash.this, RegisterPreferences.class);
                        startActivity(i);
                    } else {
                        Log.d("Service", "Sending the intent to CallCab - startupFlag of 1");
                       Intent i = new Intent (Splash.this, CallCab.class);
//                        Intent i = new Intent (Splash.this, PaymentReceipt.class);
                        //Need to pass the data retrieved from the bundle
                        i.putExtra("Flag", startupFlag);
                        i.putExtra("Name", bundle.getString("Name"));
                        i.putExtra("CreditType", bundle.getString("CreditType"));
                        i.putExtra("CreditNumber", bundle.getString("CreditNumber"));
                        i.putExtra("CCV", bundle.getString("CCV"));
                        i.putExtra("ExpireDate", bundle.getString("ExpireDate"));
                        startActivity(i);
                    }
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //STEP2: register the receiver
        if (activityReceiver != null) {   //** onDestroy is not being called here?
            //Create an intent filter to listen to the broadcast sent with the action "ACTION_STRING_ACTIVITY"
            IntentFilter intentFilter = new IntentFilter(ACTION_STRING_ACTIVITY);
            //Map the intent filter to the receiver
            registerReceiver(activityReceiver, intentFilter);
        }

        //Start the service on launching the application
        startService(new Intent(this,Taxi2MeService.class));

        Thread timer= new Thread()
        {
            public void run()
            {
                try
                {
                    //Display for 3 seconds
                    sleep(3000);
                }
                catch (InterruptedException e)
                {
                    // TODO: handle exception
                    e.printStackTrace();
                }
                finally
                {
                    //Instead of shared preferences call the socket server to see if the user has already registered
                    //Ask the service > server what the flag is
                    Intent bi = new Intent();
                    bi.setAction(ACTION_STRING_SERVICE);
                    bi.putExtra("ActionFlag", "splashStartupFlag");
                    Log.d("SplashActivity", "Sending broadcast to service - asking if user has already registered.");
                    sendBroadcast(bi);
                }
            }
        };
        timer.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.splash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Destroy Welcome_screen.java after it goes to next activity
    @Override
    protected void onPause()
    {
        super.onPause();
        finish();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service", "onDestroy");
        //STEP3: Unregister the receiver
        unregisterReceiver(activityReceiver);
    }
}
