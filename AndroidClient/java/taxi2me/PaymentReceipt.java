package com.haldane.katherine.kh_jl_taxi2me;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


public class PaymentReceipt extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_receipt);
        Bundle bundle = getIntent().getExtras();

        String cardno = "xxxxxxxxxxxx" + bundle.getString("CreditNumber").substring(12);
        ((TextView) findViewById(R.id.textViewClientName2)).setText(bundle.getString("Name"));
        ((TextView) findViewById(R.id.textViewClientCardNo2)).setText(cardno);
        ((TextView) findViewById(R.id.textViewClientCardType2)).setText(bundle.getString("CreditType"));
        ((TextView) findViewById(R.id.textViewTotalCost2)).setText(bundle.getString("Price"));
        ((TextView) findViewById(R.id.textViewPrice2)).setText(bundle.getString("Price"));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.payment_receipt, menu);
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

    public void onButtonClick(View view)
    {
        switch (view.getId()) {
            case R.id.buttonEmail:

                break;
            case R.id.buttonContinueHome:
                Intent iClear = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                iClear.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(iClear);
                break;
        }
    }
}

