package com.example.jared.clientserverandroidtoandroid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

}


public class SocketService extends Service {
    public SocketService() {
    }

    TextView textResponse;
    EditText editTextAddress, editTextPort;
    Button buttonConnect, buttonClear;

    EditText welcomeMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate();
        //setContentView(R.layout.activity_client);

        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        buttonConnect = (Button) findViewById(R.id.connect);
        buttonClear = (Button) findViewById(R.id.clear);
        textResponse = (TextView) findViewById(R.id.response);

        welcomeMsg = (EditText)findViewById(R.id.welcomemsg);

        buttonConnect.setOnClickListener(buttonConnectOnClickListener);

        buttonClear.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                textResponse.setText("");
            }
        });
    }

    OnClickListener buttonConnectOnClickListener = new OnClickListener() {

        @TargetApi(Build.VERSION_CODES.HONEYCOMB) @Override
        public void onClick(View arg0) {

//            String tMsg = welcomeMsg.getText().toString();
//            if(tMsg.equals("")){
//                tMsg = null;
//                Toast.makeText(ClientActivity.this, "No Welcome Msg sent", Toast.LENGTH_SHORT).show();
//            }

            MyClientTask myClientTask = new MyClientTask(editTextAddress
                    .getText().toString(), Integer.parseInt(editTextPort
                    .getText().toString()),
                    "");// tMsg);

   /*
    * Handle multi AsyncTask at the same time
    * Refer: http://android-er.blogspot.com/2014/04/run-multi-asynctask-as-same-time.html
    */
            //myClientTask.execute();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                myClientTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                myClientTask.execute();
        }
    };

    public class MyClientTask extends AsyncTask<Void, Void, Void> {

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
        protected Void doInBackground(Void... arg0) {

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

//                ClientActivity.this.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        Toast.makeText(ClientActivity.this,
//                                "MyClientTask ended",
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
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
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            textResponse.setText(response);
            super.onPostExecute(result);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}


