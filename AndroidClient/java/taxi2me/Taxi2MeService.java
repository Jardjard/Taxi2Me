package com.haldane.katherine.kh_jl_taxi2me;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Taxi2MeService extends Service {
    //Strings to register to create intent filter for registering the recivers
    private static final String ACTION_STRING_SERVICE = "ToService";
    private static final String ACTION_STRING_ACTIVITY = "ToActivity";
    private Socket client;
    private PrintWriter printwriter;
    private String message;

    private String servermessage = "";
    String addr = "192.168.43.232";
    int port = 8080;

    // Activity ))<>(( Service (as acting "database")
    String startupFlag;
    String activityName;
    String activityCreditType;
    String activityCreditNumber;
    String activityCCV;
    String activityExpireDate;
    boolean startServiceSharedPreferences;

    boolean taxiSentFlag = false;
    boolean taxiArrivedFlag = false;
    boolean paymentApprovedFlag = false;
    boolean placeCallFlag = false;
    boolean placeCallFlagCredit = false;
    // set these with GPS
    double clientLat = 0.0;
    double clientLong = 0.0;
    String taxiLat = "0.0";
    String taxiLong = "0.0";

    String price = "";

    //STEP1: Create a broadcast receiver
    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences settings = getSharedPreferences("state", Context.MODE_PRIVATE);
            startServiceSharedPreferences = settings.getBoolean("spStartService", false);
            if (startServiceSharedPreferences){
                //Restore the state of the shared preferences
                startupFlag = settings.getString("spStartupFlag", "");
                activityName = settings.getString("spActivityName", "");
                activityCreditType = settings.getString("spActivityCreditType", "");
                activityCreditNumber = settings.getString("spActivityCreditNumber", "");
                activityCCV = settings.getString("spActivityCCV", "");
                activityExpireDate = settings.getString("spActivityExpireDate", "");
            }
            else {
                startupFlag = "0";
                activityName = "";
                activityCreditType = "";
                activityCreditNumber = "";
                activityCCV = "";
                activityExpireDate = "";
            }

            //All the work will be done in the receiver!
            Bundle bundle = intent.getExtras();
            String ActionFlag = bundle.getString("ActionFlag");
            Intent i = new Intent();

            //Check the flag is and send certain actions/questions to the server
            //Splash Activity
            if(ActionFlag.equals("splashStartupFlag"))
            {
                if(startupFlag.equals("0")) {
                    Log.d("Service", "Sending broadcast to activity - startupFlag of 0");
                    i.setAction(ACTION_STRING_ACTIVITY);
                    i.putExtra("StartupFlag", startupFlag);
                    sendBroadcast(i);
                } else {
                    Log.d("Service", "Sending broadcast to activity - startupFlag of 1");
                    i.setAction(ACTION_STRING_ACTIVITY);
                    i.putExtra("StartupFlag", startupFlag);
                    i.putExtra("Name", activityName);
                    i.putExtra("CreditType", activityCreditType);
                    i.putExtra("CreditNumber", activityCreditNumber);
                    i.putExtra("CCV", activityCCV);
                    i.putExtra("ExpireDate", activityExpireDate);
                    sendBroadcast(i);
                }
            }
            //Register Preferences Activity
            else if(ActionFlag.equals("skipStartupFlag"))
            {
                startupFlag = "1";
                saveSharedPreferences();
            }
            else if(ActionFlag.equals("continueStartupFlag"))
            {
                startupFlag = "1";
                //Get the rest of the bundle name, ccv, etc.
                activityName = bundle.getString("Name");
                activityCreditType = bundle.getString("CreditType");
                activityCreditNumber = bundle.getString("CreditNumber");
                activityCCV = bundle.getString("CCV");
                activityExpireDate = bundle.getString("ExpireDate");
                saveSharedPreferences();
            }
            else if(ActionFlag.equals("saveNewCreditInfoFlag"))
            {
                //Update Credit Information Variables
                activityName = bundle.getString("Name");
                activityCreditType = bundle.getString("CreditType");
                activityCreditNumber = bundle.getString("CreditNumber");
                activityCCV = bundle.getString("CCV");
                activityExpireDate = bundle.getString("ExpireDate");
                saveSharedPreferences();
            }
            else if(ActionFlag.equals("cancelNewCreditInfoFlag"))
            {
                //send a broadcast from the credit info variables
                Log.d("Service", "Sending broadcast to activity - CallCab activity cancel previous credit information");
                i.setAction(ACTION_STRING_ACTIVITY);
                i.putExtra("Name", activityName);
                i.putExtra("CreditType", activityCreditType);
                i.putExtra("CreditNumber", activityCreditNumber);
                i.putExtra("CCV", activityCCV);
                i.putExtra("ExpireDate", activityExpireDate);
                i.putExtra("TaxiLatitude", taxiLat);
                i.putExtra("TaxiLongitude", taxiLong);
                sendBroadcast(i);
            }
            else if (ActionFlag.equals("networkSettings")) {
                try {
                    addr = bundle.getString("ipAddress", addr);
                    port = bundle.getInt("port", port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (ActionFlag.equals("placeCallFlag") || (!taxiSentFlag && placeCallFlag)) {
                // Set lat & long
                clientLat = bundle.getDouble("clientLatitude", 0.0);
                clientLong = bundle.getDouble("clientLongitude", 0.0);
                placeCallFlag = true;

                taxiSentFlag = false;
                taxiArrivedFlag = false;
                paymentApprovedFlag = false;

                taxiLat = "0.0";
                taxiLong = "0.0";

                price = "";

                String msg = "NEWCALL:" + clientLat + ":" + clientLong;
                if (bundle.getString("PaymentType").equals("cash")) {
                    placeCallFlagCredit = false;
                }
                if (bundle.getString("PaymentType").equals("CreditPhone") || placeCallFlagCredit) {
                    placeCallFlagCredit = true;
                    msg += ":" + activityName + ":" + activityCreditType + ":" + activityCreditNumber + ":" + activityCCV + ":" + activityExpireDate;
                }
                if (taxiSentFlag) {
                    msg = "UPDATE:" + clientLat + ":" + clientLong;
                }
                MyClientTask sendMyClientTask = new MyClientTask(addr, port, msg);
                sendMyClientTask.execute();
            }
            else if (ActionFlag.equals("getUpdate")) {
                try {
                    clientLat = bundle.getDouble("clientLatitude", 0.0);
                    clientLong = bundle.getDouble("clientLongitude", 0.0);
                    String msg = "UPDATE:" + clientLat + ":" + clientLong;
                    MyClientTask sendMyClientTask = new MyClientTask(addr, port, msg);
                    sendMyClientTask.execute();

                    i.setAction(ACTION_STRING_ACTIVITY);
                    i.putExtra("Name", activityName);
                    i.putExtra("CreditType", activityCreditType);
                    i.putExtra("CreditNumber", activityCreditNumber);
                    i.putExtra("CCV", activityCCV);
                    i.putExtra("ExpireDate", activityExpireDate);
                    i.putExtra("TaxiLatitude", taxiLat);
                    i.putExtra("TaxiLongitude", taxiLong);
                    i.putExtra("PaymentType", placeCallFlagCredit?"CreditPhone":"cash");

                    if (paymentApprovedFlag) {
                        i.putExtra("MessageType", "PaymentApproved");
                    }
                    else if (price != "") {
                        i.putExtra("MessageType", "RequestPayment");
                        i.putExtra("Price", price);
                    } else {
                        i.putExtra("MessageType", "Update");
                    }

                    sendBroadcast(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Service", "onCreate");
        //STEP2: register the receiver
        if (serviceReceiver != null) {
            //Create an intent filter to listen to the broadcast sent with the action "ACTION_STRING_SERVICE"
            IntentFilter intentFilter = new IntentFilter(ACTION_STRING_SERVICE);
            //Map the intent filter to the receiver
            registerReceiver(serviceReceiver, intentFilter);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service", "onDestroy");
        saveSharedPreferences();
        //STEP3: Unregister the receiver
        unregisterReceiver(serviceReceiver);
    }

    public void saveSharedPreferences()
    {
        startServiceSharedPreferences = true;
        SharedPreferences settings = getSharedPreferences("state", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("spStartService", startServiceSharedPreferences);
        editor.putString("spStartupFlag", startupFlag);
        editor.putString("spActivityName",activityName);
        editor.putString("spActivityCreditType", activityCreditType);
        editor.putString("spActivityCreditNumber",activityCreditNumber);
        editor.putString("spActivityCCV", activityCCV);
        editor.putString("spActivityExpireDate",activityExpireDate);
        editor.commit();
    }

    public class MyClientTask extends AsyncTask<Void, Void, String> {

        String dstAddress;
        int dstPort;
        String response = "";
        String msgToServer;

        MyClientTask(String addr, int port, String msgTo) {
            dstAddress = addr;
            dstPort = port;
            msgToServer = msgTo;
        }

        @Override
        protected String doInBackground(Void... arg0) {

            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            DataInputStream dataInputStream = null;

            try {
                socket = new Socket(dstAddress, dstPort);
                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                if(msgToServer != null){
                    dataOutputStream.writeUTF(msgToServer);
                }

                //If dataInputStream is empty,
                //readUTF() will block the current thread,
                //but not the UI thread (or main thread).
                response = dataInputStream.readUTF();

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "IOException: " + e.toString();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "IOException: " + e.toString();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {

            Log.d("Service", "ServerMessage: " + servermessage + " Result: " + result);
            if (result.length() > 0) {
                String[] resultSet = result.split(":");
                if(result.equals("NoTaxi")) {
                    taxiSentFlag = true;
                }
                else if(resultSet[0].equals("Location")) {
                    taxiSentFlag = true;
                    taxiLat = resultSet[1];
                    taxiLong = resultSet[2];
                    Log.d("Service", "TaxiLocation: " + taxiLat + ":" + taxiLong + " Result: " + result);
                    if (taxiLat.equals("0") && taxiLong.equals("0")) {
                        taxiArrivedFlag = true;
                    }
                }
                else if(resultSet[0].equals("Receipt")) {
                    taxiSentFlag = true;
                    price = resultSet[1];
                    Log.d("Service", "Receipt: " + price + " Result: " + result);
                }
                else if(resultSet[0].equals("ApprovedPayment")) {
                    taxiSentFlag = true;
                    paymentApprovedFlag = true;
                }
            }
            super.onPostExecute(result);
        }

    }

}
