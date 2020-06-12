package cordova.woosim.printer;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.woosim.printer.WoosimCmd;
import com.woosim.printer.WoosimService;

public class MainActivity extends Activity {
    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    // Message types sent from the BluetoothPrintService Handler
    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_READ = 3;

    // Key names received from the BluetoothPrintService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the print services
    private BluetoothPrintService mPrintService = null;
    private WoosimService mWoosim = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        setContentView(R.layout.activity_main);
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            if(D) Log.e(TAG, "+++ BLUETOOTH NOT SUPPORTED +++");
            Toast.makeText(this, R.string.toast_bt_na, Toast.LENGTH_LONG).show();
            finish();
            return;
        } else {
            if(D) Log.e(TAG, "+++ BLUETOOTH SUPPORTED +++");
        }


        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else {
                if(D) Log.e(TAG, "+++ MIMETYPE NOT SUPPORTED:  " + type  + " +++");
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared

            if(D) Log.e(TAG, "+++ PRINT TEXT:  " + sharedText  + " +++");

            // print the text
            // this.print( sharedText );

        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupPrint() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mPrintService == null) setupPrint();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mPrintService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mPrintService.getState() == BluetoothPrintService.STATE_NONE) {
              // Start the Bluetooth print services
            	mPrintService.start();
            }
        }
    }

    private void setupPrint() {
        if(D) Log.d(TAG, "setupPrint()");

        Intent serverIntent = null;
        serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);

        // Initialize the BluetoothPrintService to perform bluetooth connections
        mPrintService = new BluetoothPrintService(this, mHandler);
        mWoosim = new WoosimService(mHandler);
    }

    // The Handler that gets information back from the BluetoothPrintService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getInt(TOAST), Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_READ:
                mWoosim.processRcvData((byte[])msg.obj, msg.arg1);
                break;
            case WoosimService.MESSAGE_PRINTER:
            	switch (msg.arg1) {
            	case WoosimService.MSR:
            		Log.d(TAG, "MSR");
            		if (msg.arg2 == 0) {
            			Toast.makeText(getApplicationContext(), "MSR reading failure", Toast.LENGTH_SHORT).show();
            		} else {
                    	byte[][] track = (byte[][])msg.obj;
            		}
                	break;
            	}
            	break;
            }
        }
    };

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth print services
        if (mPrintService != null) mPrintService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a print
            	setupPrint();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mPrintService.connect(device, secure);
    }
    
    /**
     * Print data.
     * @param data  A byte array to print.
     */
    private void sendData(byte[] data) {
        // Check that we're actually connected before trying printing
        if (mPrintService.getState() != BluetoothPrintService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check that there's actually something to send
        if (data.length > 0) 
        	mPrintService.write(data);
    }


    
    public void printText(View v) {
    	EditText editText = (EditText) findViewById(R.id.edit_text);
    	String string = editText.getText().toString();
    	byte[] text = string.getBytes();
    	if (text.length == 0) return;
    	
    	ByteArrayBuffer buffer = new ByteArrayBuffer(1024);
    	
    	byte[] cmd1 = WoosimCmd.setTextStyle(false, false, false, 1, 1);
    	byte[] cmd2 = WoosimCmd.setTextAlign( 1 );
    	byte[] cmd3 = WoosimCmd.printData();
    	
    	buffer.append(cmd1, 0, cmd1.length);
    	buffer.append(cmd2, 0, cmd2.length);
    	buffer.append(text, 0, text.length);
    	buffer.append(cmd3, 0, cmd3.length);
    	
    	sendData(WoosimCmd.initPrinter());
    	sendData(buffer.toByteArray());
    }

    public void print( String text2print ) {

        byte[] text = text2print.getBytes();
        if (text.length == 0) return;

        ByteArrayBuffer buffer = new ByteArrayBuffer(1024);

        byte[] cmd1 = WoosimCmd.setTextStyle(false, false, false, 1, 1);
        byte[] cmd2 = WoosimCmd.setTextAlign( 1 );
        byte[] cmd3 = WoosimCmd.printData();

        buffer.append(cmd1, 0, cmd1.length);
        buffer.append(cmd2, 0, cmd2.length);
        buffer.append(text, 0, text.length);
        buffer.append(cmd3, 0, cmd3.length);

        sendData(WoosimCmd.initPrinter());
        sendData(buffer.toByteArray());
    }
    


}
