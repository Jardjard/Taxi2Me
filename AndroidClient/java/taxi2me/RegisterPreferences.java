package com.haldane.katherine.kh_jl_taxi2me;


import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class RegisterPreferences extends Activity {
    //Strings to register to create intent filter for registering the recivers
    private static final String ACTION_STRING_SERVICE = "ToService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_preferences);

        //Set up the spinner
        final String[] creditCardType = {"Visa","Mastercard","American Express","Discover", "JCB"};

        Spinner cardType = (Spinner) findViewById(R.id.spinnerCardType);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, creditCardType);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cardType.setAdapter(dataAdapter);

        //Set up the icons (arrow, calendar)
        Typeface fontFamily = Typeface.createFromAsset(getAssets(), "fonts/fontawesome-webfont.ttf");
        TextView arrow = (TextView) findViewById(R.id.arrow);
        arrow.setTypeface(fontFamily);
        arrow.setText("\uf063");

        TextView calendar = (TextView) findViewById(R.id.calendar);
        calendar.setTypeface(fontFamily);
        calendar.setText("\uf073");

        DatePicker datepicker= (DatePicker)findViewById(R.id.datePickerFrag);
        LinearLayout v1=(LinearLayout)datepicker.getChildAt(0);
        LinearLayout v2=(LinearLayout)v1.getChildAt(0);
        View v3=v2.getChildAt(1);
        v3.setVisibility(View.GONE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.register_preferences, menu);
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
        Intent CallCabI = new Intent(this, CallCab.class);
        RelativeLayout datePicker = (RelativeLayout) findViewById(R.id.dateLayout);
        RelativeLayout dateButtons = (RelativeLayout) findViewById(R.id.dateButtons);
        EditText name= (EditText) findViewById(R.id.editTextName);
        EditText ccn= (EditText) findViewById(R.id.editTextCreditCardNumber);
        EditText ccv= (EditText) findViewById(R.id.editTextCcv);
        EditText date= (EditText) findViewById(R.id.editTextDate);
        Spinner cardType = (Spinner) findViewById(R.id.spinnerCardType);
        Button skip = (Button) findViewById(R.id.buttonSkip);
        Button next = (Button) findViewById(R.id.buttonContinue);

        Intent i= new Intent();

        switch (view.getId()) {
            case R.id.buttonSkip:
                //Send a message to the service with details
                i.setAction(ACTION_STRING_SERVICE);
                i.putExtra("ActionFlag", "skipStartupFlag");
                Log.d("SampleActivity", "Sending broadcast to service - Skipp button has been clicked!");
                sendBroadcast(i);

               CallCabI.putExtra("Flag", "0");
               startActivity(CallCabI);
                break;
            case R.id.buttonContinue:
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
                    //Add to Shared Preferences
                    try {
                        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                        editor.putString("Name", name.getText().toString());
                        editor.putString("CreditType", cardType.getSelectedItem().toString());
                        editor.putString("CreditNumber", ccn.getText().toString());
                        editor.putString("CCV", ccv.getText().toString());
                        editor.putString("ExpireDate", date.getText().toString());
                        editor.commit();
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(getApplicationContext(), "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
                    }
                    //Send The Information to the server
                    //Send a mesage to the service to talk to the server
                    Intent serverI = new Intent();
                    serverI.setAction(ACTION_STRING_SERVICE);
                    serverI.putExtra("ActionFlag", "continueStartupFlag");
                    serverI.putExtra("Name", name.getText().toString());
                    serverI.putExtra("CreditType", cardType.getSelectedItem().toString());
                    serverI.putExtra("CreditNumber", ccn.getText().toString());
                    serverI.putExtra("CCV", ccv.getText().toString());
                    serverI.putExtra("ExpireDate", date.getText().toString());
                    Log.d("SampleActivity", "Sending broadcast to service - continue button");
                    sendBroadcast(serverI);
                    //Go to call cab page

                    //Go to call cab page
                    CallCabI.putExtra("Name", name.getText().toString());
                    CallCabI.putExtra("CreditType", cardType.getSelectedItem().toString());
                    CallCabI.putExtra("CreditNumber", ccn.getText().toString());
                    CallCabI.putExtra("CCV", ccv.getText().toString());
                    CallCabI.putExtra("ExpireDate", date.getText().toString());
                    CallCabI.putExtra("Flag", "1");
                    startActivity(CallCabI);
                }

                break;
            case R.id.buttonSet:
                DatePicker datePicked = (DatePicker) findViewById(R.id.datePickerFrag);
                EditText setDate= (EditText) findViewById(R.id.editTextDate);
                setDate.setText(datePicked.getYear() + "/" + (datePicked.getMonth()+ 1));
                datePicker.setVisibility(View.GONE);
                dateButtons.setVisibility(View.GONE);
                date.setVisibility(View.VISIBLE);
                skip.setVisibility(View.VISIBLE);
                next.setVisibility(View.VISIBLE);
                break;
            case R.id.buttonCancel:
                datePicker.setVisibility(View.GONE);
                dateButtons.setVisibility(View.GONE);
                date.setVisibility(View.VISIBLE);
                skip.setVisibility(View.VISIBLE);
                next.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void onClick(View view)
    {
        RelativeLayout datePicker = (RelativeLayout) findViewById(R.id.dateLayout);
        RelativeLayout dateButtons = (RelativeLayout) findViewById(R.id.dateButtons);
        EditText date= (EditText) findViewById(R.id.editTextDate);
        Button skip = (Button) findViewById(R.id.buttonSkip);
        Button next = (Button) findViewById(R.id.buttonContinue);
        switch(view.getId())
        {
            case R.id.editTextDate:
                //open new calendar fragment
                datePicker.setVisibility(View.VISIBLE);
                dateButtons.setVisibility(View.VISIBLE);
                date.setVisibility(View.GONE);
                skip.setVisibility(View.GONE);
                next.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    protected void onPause() {
        EditText name= (EditText) findViewById(R.id.editTextName);
        EditText ccn= (EditText) findViewById(R.id.editTextCreditCardNumber);
        EditText ccv= (EditText) findViewById(R.id.editTextCcv);
        EditText date= (EditText) findViewById(R.id.editTextDate);
        Spinner cardType = (Spinner) findViewById(R.id.spinnerCardType);
        super.onPause();
        SharedPreferences settings = getSharedPreferences("state", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("Key", "1");
        editor.putString("Name", name.getText().toString());
        editor.putString("CreditType", cardType.getSelectedItem().toString());
        editor.putString("CreditNumber", ccn.getText().toString());
        editor.putString("CCV", ccv.getText().toString());
        editor.putString("ExpireDate", date.getText().toString());
        editor.commit();
    }
}
