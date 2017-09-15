package com.example.fiyinfolu.cgsdisplay;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class DisplayControl extends AppCompatActivity {

    Button sendToDisplayButton;
    RadioButton cgsRadioButton, chrRadioButton;
    NumberPicker hNumber, tNumber, uNumber;
    RadioGroup optionRadioGroup;
    boolean isCGSMode = true;
    TextView infoTextView;

    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public void onRadioButtonClicked(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.cgsRadioButton:
                Log.i("INFORMATION", "CGS MODE ACTIVATED");
                isCGSMode = true;

                hNumber.setValue(0);
                tNumber.setValue(0);
                uNumber.setValue(0);

                hNumber.setMaxValue(7);
                tNumber.setMaxValue(9);
                uNumber.setMaxValue(9);

                hNumber.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        if (newVal >= 7) {
                            hNumber.setMaxValue(7);
                            tNumber.setMaxValue(0);
                            uNumber.setMaxValue(0);
                        } else {
                            hNumber.setMaxValue(7);
                            tNumber.setMaxValue(9);
                            uNumber.setMaxValue(9);
                        }
                    }
                });
                updateInfoTextView();
                break;
            case R.id.chrsRadioButton:
                Log.i("INFORMATION", "CHORUS MODE ACTIVATED");
                isCGSMode = false;

                hNumber.setValue(0);
                tNumber.setValue(0);
                uNumber.setValue(0);

                hNumber.setMaxValue(1);
                tNumber.setMaxValue(9);
                uNumber.setMaxValue(9);

                hNumber.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                        if (newVal >= 1) {
                            hNumber.setMaxValue(1);
                            tNumber.setMaxValue(0);
                            uNumber.setMaxValue(0);
                        } else {
                            hNumber.setMaxValue(1);
                            tNumber.setMaxValue(9);
                            uNumber.setMaxValue(9);
                        }
                    }
                });
                updateInfoTextView();
                break;
        }
    }

    public void updateInfoTextView()
    {
        int number = hNumber.getValue() *100 + tNumber.getValue() * 10 + uNumber.getValue();

        if (number == 0) {
            uNumber.setValue(1);
        }

        number = hNumber.getValue() *100 + tNumber.getValue() * 10 + uNumber.getValue();

        if (isCGSMode) {
            infoTextView.setText("CGS " + number);
        } else {
            infoTextView.setText("CHORUS " + number);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_control);

        cgsRadioButton = (RadioButton) findViewById(R.id.cgsRadioButton);
        chrRadioButton = (RadioButton) findViewById(R.id.chrsRadioButton);
        hNumber = (NumberPicker) findViewById(R.id.hNumPicker);
        tNumber = (NumberPicker) findViewById(R.id.tNumPicker);
        uNumber = (NumberPicker) findViewById(R.id.uNumPicker);
        optionRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        infoTextView = (TextView) findViewById(R.id.infoTextView);
        sendToDisplayButton = (Button) findViewById(R.id.sendToDispButton);


        isCGSMode = true;
        cgsRadioButton.setChecked(isCGSMode);

        hNumber.setMaxValue(7);
        tNumber.setMaxValue(9);
        uNumber.setMaxValue(9);

        hNumber.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if (newVal >= 7) {
                    hNumber.setMaxValue(7);
                    tNumber.setMaxValue(0);
                    uNumber.setMaxValue(0);
                }
                else {
                    hNumber.setMaxValue(7);
                    tNumber.setMaxValue(9);
                    uNumber.setMaxValue(9);
                }
                updateInfoTextView();
            }
        });

        tNumber.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateInfoTextView();
            }
        });

        uNumber.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateInfoTextView();
            }
        });

        hNumber.setMinValue(0);
        tNumber.setMinValue(0);
        uNumber.setMinValue(0);
        uNumber.setValue(1);

        //receive the address of the bluetooth device
        Intent newIntent = getIntent();
        address = newIntent.getStringExtra(MainActivity.EXTRA_ADDRESS);

        new ConnectBT().execute();

        sendToDisplayButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try {

                    int number = hNumber.getValue() *100 + tNumber.getValue() * 10 + uNumber.getValue();
                    updateInfoTextView();

                    String selectedHymnAndModeString = "";

                    selectedHymnAndModeString = Integer.toString(number);

                    if (isCGSMode) {
                        selectedHymnAndModeString = selectedHymnAndModeString + "1";
                    }
                    else {
                        selectedHymnAndModeString = selectedHymnAndModeString + "0";
                    }

                    sendMessage(selectedHymnAndModeString);

                    Log.i("INFORMATION", "Sending ..... " + selectedHymnAndModeString );

                }
                catch (Exception e) {
                    System.out.print(e);
                }
            }
        });

        updateInfoTextView();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(DisplayControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice myBluetoothRemoteDevice = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = myBluetoothRemoteDevice.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    private void disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (!btSocket.isConnected()) {
            Toast.makeText(this, "NOT CONNECTED", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            write(message);
        }
    }

    private void write(String numberToDisplay)
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(numberToDisplay.toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }
}
