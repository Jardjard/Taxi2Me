package com.haldane.katherine.kh_jl_taxi2me;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class CallCab extends Activity {
    //Strings to register to create intent filter for registering the recivers
    private static final String ACTION_STRING_SERVICE = "ToService";
    private static final String ACTION_STRING_ACTIVITY = "ToActivity";


    //Methods that are needed to talk to the service
    //STEP1: Create a broadcast receiver
    private BroadcastReceiver activityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            //When the cancel button has been called repopulate the edittext with previous data
            EditText name = (EditText) findViewById(R.id.editName);
            EditText cardType = (EditText) findViewById(R.id.editCardType);
            EditText ccv = (EditText) findViewById(R.id.editCCV);
            EditText date = (EditText) findViewById(R.id.editDate);
            EditText ccn = (EditText) findViewById(R.id.editCardNumber);

            name.setText( bundle.getString("Name"));
            cardType.setText( bundle.getString("CreditType"));
            ccv.setText( bundle.getString("CCV"));
            date.setText( bundle.getString("ExpireDate"));
            ccn.setText( bundle.getString("CreditNumber"));
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_cab);
        EditText editTextPrefNum = (EditText) findViewById(R.id.editTextPrefNum);
        ((EditText) findViewById(R.id.ipAddress)).setText("192.168.43.232");
        ((EditText) findViewById(R.id.portNumber)).setText("8080");
        editTextPrefNum.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

//        //STEP2: register the receiver
        if (activityReceiver != null) {
            //Create an intent filter to listen to the broadcast sent with the action "ACTION_STRING_ACTIVITY"
            IntentFilter intentFilter = new IntentFilter(ACTION_STRING_ACTIVITY);
            //Map the intent filter to the receiver
            registerReceiver(activityReceiver, intentFilter);
        }

        //Start the service on launching the application
        startService(new Intent(this,Taxi2MeService.class));

        //Set ASCII images (edit, delete, cancel)
        Typeface fontFamily = Typeface.createFromAsset(getAssets(), "fonts/fontawesome-webfont.ttf");
        TextView edit = (TextView) findViewById(R.id.editIcon);
        edit.setTypeface(fontFamily);
        edit.setText("\uf040");

        TextView cancel = (TextView) findViewById(R.id.cancelIcon);
        cancel.setTypeface(fontFamily);
        cancel.setText("\uf05e");

        TextView save = (TextView) findViewById(R.id.saveIcon);
        save.setTypeface(fontFamily);
        save.setText("\uf0c7");

        //Populate the spinner
        final String[] creditCardType = {"Visa","Mastercard","American Express","Discover", "JCB"};
        Spinner cardType = (Spinner) findViewById(R.id.spinnerEditCard);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, creditCardType);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cardType.setAdapter(dataAdapter);

        //set edit text disabled
        EditText name = (EditText) findViewById(R.id.editName);
        EditText ccv = (EditText) findViewById(R.id.editCCV);
        EditText ccn = (EditText) findViewById(R.id.editCardNumber);
        setEditableOff(name);
        setEditableOff(ccn);
        setEditableOff(ccv);

        DatePicker datepicker= (DatePicker)findViewById(R.id.editDatePicker);
        LinearLayout v1=(LinearLayout)datepicker.getChildAt(0);
        LinearLayout v2=(LinearLayout)v1.getChildAt(0);
        View v3=v2.getChildAt(1);
        v3.setVisibility(View.GONE);

        populatePreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.call_cab, menu);
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

    public void onButtonClick(View view) {
        EditText callPref = (EditText) findViewById(R.id.editTextPrefNum);
        EditText name = (EditText) findViewById(R.id.editName);
        EditText cardType = (EditText) findViewById(R.id.editCardType);
        EditText ccv = (EditText) findViewById(R.id.editCCV);
        EditText date = (EditText) findViewById(R.id.editDate);
        EditText ccn = (EditText) findViewById(R.id.editCardNumber);
        LinearLayout show = (LinearLayout) findViewById(R.id.showLayout);
        Intent CabMap = new Intent(this, TaxiMap.class);
        switch (view.getId()) {
            case R.id.buttonCallCab:
                Intent iNetwork = new Intent();
                iNetwork.setAction(ACTION_STRING_SERVICE);
                iNetwork.putExtra("ActionFlag", "networkSettings");
                iNetwork.putExtra("ipAddress", ((EditText) findViewById(R.id.ipAddress)).getText().toString());
                iNetwork.putExtra("port", Integer.parseInt( ( (EditText) findViewById(R.id.portNumber) ).getText().toString() ) );
                Log.d("SampleActivity", "Sending broadcast to service");
                sendBroadcast(iNetwork);
                show.setVisibility(View.VISIBLE);
                break;
            case R.id.buttonCall:
                RadioButton cash = (RadioButton) findViewById(R.id.radioButtonCash);
                RadioButton creditPhone = (RadioButton) findViewById(R.id.radioButtonCreditByPhone);

                String paymentType = "";

                //Find which radio button was clicked
                if(cash.isChecked())
                {
                    paymentType = "cash";
                    if(callPref.getText().toString().trim().length() != 14)
                    {
                        Toast.makeText(getApplicationContext(), "Please Enter a phone number with area code! Eg. 5195551234", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        //Get the current GPS location
                        GPSService mGPSService = new GPSService(getBaseContext());
                        mGPSService.getLocation();
                        if (mGPSService.isLocationAvailable == false) {
                            Toast.makeText(getBaseContext(), "Your location is not available, please turn on GPS!.", Toast.LENGTH_SHORT).show();
                            mGPSService.closeGPS();
                            return;
                        } else {
                            // Getting location co-ordinates
                            double latitude = mGPSService.getLatitude();
                            double longitude = mGPSService.getLongitude();
                            mGPSService.closeGPS();
                            Intent i = new Intent();
                            i.setAction(ACTION_STRING_SERVICE);
                            i.putExtra("PaymentType", paymentType);
                            i.putExtra("ActionFlag", "placeCallFlag");
                            i.putExtra("clientLatitude", latitude);
                            i.putExtra("clientLongitude", longitude);
                            i.putExtra("CallPref", callPref.getText().toString());
                            Log.d("SampleActivity", "Sending broadcast to service");
                            sendBroadcast(i);
                            startActivity(CabMap);
                        }
                    }
                }
                else if (creditPhone.isChecked())
                {
                    paymentType = "CreditPhone";

                    if (validateCallCabForm()){
                        //Instead of passing intent to cabMap
                        //Send a broadcast to the service to go to the server
                        //Get the current GPS location
                        GPSService mGPSService = new GPSService(getBaseContext());
                        mGPSService.getLocation();
                        if (mGPSService.isLocationAvailable == false) {
                            Toast.makeText(getBaseContext(), "Your location is not available, please turn on GPS!.", Toast.LENGTH_SHORT).show();
                            mGPSService.closeGPS();
                            return;
                        } else {
                            // Getting location co-ordinates
                            double latitude = mGPSService.getLatitude();
                            double longitude = mGPSService.getLongitude();
                            mGPSService.closeGPS();
                            Intent i = new Intent();
                            i.setAction(ACTION_STRING_SERVICE);
                            i.putExtra("Name", name.getText().toString());
                            i.putExtra("CreditType", cardType.toString());
                            i.putExtra("CreditNumber", ccn.getText().toString());
                            i.putExtra("CCV", ccv.getText().toString());
                            i.putExtra("ExpireDate", date.getText().toString());
                            i.putExtra("PaymentType", paymentType);
                            i.putExtra("ActionFlag", "placeCallFlag");
                            i.putExtra("CallPref", callPref.getText().toString());
                            i.putExtra("clientLatitude", latitude);
                            i.putExtra("clientLongitude", longitude);
                            Log.d("SampleActivity", "Sending broadcast to service");
                            sendBroadcast(i);
                            startActivity(CabMap);
                        }
                    }
                }
                break;
            case R.id.buttonCancelCall:
                show.setVisibility(View.GONE);
                break;
        }
    }

    public void populatePreferences()
    {

        EditText callPref = (EditText) findViewById(R.id.editTextPrefNum);
        EditText name = (EditText) findViewById(R.id.editName);
        EditText cardType = (EditText) findViewById(R.id.editCardType);
        EditText ccv = (EditText) findViewById(R.id.editCCV);
        EditText date = (EditText) findViewById(R.id.editDate);
        EditText ccn = (EditText) findViewById(R.id.editCardNumber);

        //Get intent and pass it to populate preferences
        Bundle bundle = getIntent().getExtras();
        String  IName = bundle.getString("Name");
        String  ICreditType = bundle.getString("CreditType");
        String  ICreditNumber = bundle.getString("CreditNumber");
        String  ICCV = bundle.getString("CCV");
        String  IExpireDate = bundle.getString("ExpireDate");
        String  IFlag = bundle.getString("Flag");

        //Populate the edit preferences window!
        EditText expireDate = (EditText) findViewById(R.id.editDate);
        EditText etCardType = (EditText) findViewById(R.id.editCardType);

        if(IFlag.equals("1")) {
            name.setText(IName);
            ccn.setText(ICreditNumber);
            ccv.setText(ICCV);
            expireDate.setText(IExpireDate);
            etCardType.setText(ICreditType);
        }
    }

    public void onClick(View view)
    {
        Intent i = new Intent();

        EditText name = (EditText) findViewById(R.id.editName);
        Spinner cardType = (Spinner) findViewById(R.id.spinnerEditCard);
        EditText ccv = (EditText) findViewById(R.id.editCCV);
        EditText ccn = (EditText) findViewById(R.id.editCardNumber);
        ArrayAdapter myAdap = (ArrayAdapter) cardType.getAdapter();
        //EditText Boxes
        EditText expireDate = (EditText) findViewById(R.id.editDate);
        EditText etCardType = (EditText) findViewById(R.id.editCardType);
        DatePicker dpExpire = (DatePicker) findViewById(R.id.editDatePicker);

        Button btnCallCab = (Button) findViewById(R.id.buttonCall);
        Button btnCancel = (Button) findViewById(R.id.buttonCancelCall);

        //Icons
        TextView edit = (TextView) findViewById(R.id.editIcon);
        TextView save = (TextView) findViewById(R.id.saveIcon);
        TextView cancel = (TextView) findViewById(R.id.cancelIcon);
        LinearLayout show = (LinearLayout) findViewById(R.id.creditInfo);
        switch (view.getId()) {
            case R.id.radioButtonCreditByPhone:
                show.setVisibility(View.VISIBLE);
                break;
            case R.id.radioButtonCash:
                show.setVisibility(View.GONE);
                break;
            case R.id.editIcon:
                //Set everything to editable
                if (!etCardType.getText().toString().trim().isEmpty())
                {
                    int spinnerPosition = myAdap.getPosition(etCardType.getText().toString());
                    cardType.setSelection(spinnerPosition);
                }
                else {
                    int spinnerPosition = myAdap.getPosition("Visa");
                    cardType.setSelection(spinnerPosition);
                }

                //Set the datepicker
                EditText setDate = (EditText) findViewById(R.id.editDate);
                if(!setDate.getText().toString().trim().isEmpty()) {
                    //parse string setDate by delimiter or /
                    String[] date = setDate.getText().toString().split("/");
                    dpExpire.updateDate(Integer.parseInt(date[0]), Integer.parseInt(date[1]), 1);

                }

                setEditableOn(name);
                setEditableOn(ccn);
                setEditableOn(ccv);

                btnCallCab.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
                dpExpire.setVisibility(View.VISIBLE);
                expireDate.setVisibility(View.GONE);
                etCardType.setVisibility(View.GONE);
                cardType.setVisibility(View.VISIBLE);

                //Display/Hide buttons
                edit.setVisibility(View.GONE);
                save.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                break;
            case R.id.cancelIcon:
                setEditableOff(name);
                setEditableOff(ccn);
                setEditableOff(ccv);

                //Send a broadcast asking for what was previously in the credit information fields
                i.setAction(ACTION_STRING_SERVICE);
                i.putExtra("ActionFlag", "cancelNewCreditInfoFlag");
                Log.d("SampleActivity", "Sending broadcast to service - Cancel new credit info has been clicked!");
                sendBroadcast(i);

                expireDate.setVisibility(View.VISIBLE);
                btnCallCab.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);

                edit.setVisibility(View.VISIBLE);
                save.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);

                dpExpire.setVisibility(View.GONE);
                ccv.setVisibility(View.VISIBLE);
                etCardType.setVisibility(View.VISIBLE);
                cardType.setVisibility(View.GONE);

                //add the old values from the bundle
                break;
            case R.id.saveIcon:
                //Valudation
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
                else {
                    //Send broadcast to the server
                    i.setAction(ACTION_STRING_SERVICE);
                    i.putExtra("ActionFlag", "saveNewCreditInfoFlag");
                    i.putExtra("Name", name.getText().toString());
                    i.putExtra("CreditType",etCardType.getText().toString());
                    i.putExtra("CreditNumber",ccn.getText().toString());
                    i.putExtra("CCV",ccv.getText().toString());
                    i.putExtra("ExpireDate",expireDate.getText().toString());
                    Log.d("SampleActivity", "Sending broadcast to service - Save new credit info has been clicked!");
                    sendBroadcast(i);

                    //disable everything
                    setEditableOff(name);
                    setEditableOff(ccn);
                    setEditableOff(ccv);

                    //disable creditcard date & type
                    Spinner spinnerCardType = (Spinner) findViewById(R.id.spinnerEditCard);
                    EditText editCardType = (EditText) findViewById(R.id.editCardType);
                    DatePicker datePicked = (DatePicker) findViewById(R.id.editDatePicker);
                    setDate = (EditText) findViewById(R.id.editDate);
                    setDate.setText(datePicked.getYear() + "/" + (datePicked.getMonth() + 1));
                    //Disable spinner and display
                    editCardType.setText(spinnerCardType.getSelectedItem().toString());
                    btnCallCab.setVisibility(View.VISIBLE);
                    btnCancel.setVisibility(View.VISIBLE);
                    editCardType.setVisibility(View.VISIBLE);
                    spinnerCardType.setVisibility(View.GONE);
                    dpExpire.setVisibility(View.GONE);
                    save.setVisibility(View.GONE);
                    cancel.setVisibility(View.GONE);
                    edit.setVisibility(View.VISIBLE);
                    ccv.setVisibility(View.VISIBLE);
                    expireDate.setVisibility(View.VISIBLE);
                    break;
                }
        }
    }

    public void setEditableOn(EditText et){
        et.setLongClickable(true);
        et.setClickable(true);
        et.setCursorVisible(true);
        et.setFocusable(true);
        et.setClickable(true);
        et.setEnabled(true);
        et.setFocusable(true);
        et.setFocusableInTouchMode(true);
        et.requestFocus();
    }

    public void setEditableOff(EditText et){
        et.setLongClickable(false);
        et.setClickable(false);
        et.setCursorVisible(false);
        et.setTextColor(Color.parseColor("#000000"));
        et.setFocusable(false);
        et.setClickable(false);
        et.setEnabled(false);
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.requestFocus();
    }

    public boolean validateCallCabForm()
    {
        EditText callPref = (EditText) findViewById(R.id.editTextPrefNum);
        EditText name = (EditText) findViewById(R.id.editName);
        EditText cardType = (EditText) findViewById(R.id.editCardType);
        EditText ccv = (EditText) findViewById(R.id.editCCV);
        EditText date = (EditText) findViewById(R.id.editDate);
        EditText ccn = (EditText) findViewById(R.id.editCardNumber);

        if(name.getText().toString().trim().length() == 0)
        {
            Toast.makeText(getApplicationContext(), "Please Enter a Name!", Toast.LENGTH_SHORT).show();
            return false;
        }
        //Validate Credit Card Number
        else if (ccn.getText().length() != 16)
        {
            Toast.makeText(getApplicationContext(), "Invalid Credit Card Number! Must be 16 digits! ", Toast.LENGTH_SHORT).show();
            return false;
        }
        //Validate ccv
        else if (ccv.getText().length() != 3)
        {
            Toast.makeText(getApplicationContext(), "Invalid CCV! Must be 3 digits!", Toast.LENGTH_SHORT).show();
            return false;
        }
        //Validate Date
        else if(date.getText().toString().trim().length() == 0)
        {
            Toast.makeText(getApplicationContext(), "Please Enter a Date!", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(cardType.getText().toString().trim().length() == 0)
        {
            Toast.makeText(getApplicationContext(), "Please Enter a Card Type!", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if(callPref.getText().toString().trim().length() != 14)
        {
            Toast.makeText(getApplicationContext(), "Please Enter a phone number with area code! Eg. 5195555555", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service", "onDestroy");
        //STEP3: Unregister the receiver
        unregisterReceiver(activityReceiver);
    }
}
