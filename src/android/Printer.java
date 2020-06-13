
package cordova.woosim.printer;

//
// some of the code you wil find here is not nessessary
// we never cleaned it up
// so just ignore it
//  but basically it works for woosim printer
//

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
// import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.os.Environment;
import android.widget.EditText;
import android.widget.Toast;
import android.media.MediaScannerConnection;

import com.woosim.printer.WoosimCmd;
import com.woosim.printer.WoosimService;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

public class Printer extends CordovaPlugin {

    private CordovaInterface cordova;
    private CallbackContext ctx;

    private String printAppIds[] = {
		"com.dynamixsoftware.printershare",			// Printer Share
        "kr.co.iconlab.BasicPrintingProfile",       // Bluetooth Smart Printing
        "com.blueslib.android.app",                 // Bluetooth SPP Printer API
        "com.brother.mfc.brprint",                  // Brother iPrint&Scan
        "com.brother.ptouch.sdk",                   // Brother Print Library
        "jp.co.canon.bsd.android.aepp.activity",    // Canon Easy-PhotoPrint
        "com.pauloslf.cloudprint",                  // Cloud Print
        "com.dlnapr1.printer",                      // CMC DLNA Print Client
        "com.dell.mobileprint",                     // Dell Mobile Print
        "com.printjinni.app.print",                 // PrintJinni
        "epson.print",                              // Epson iPrint
        "jp.co.fujixerox.prt.PrintUtil.PCL",        // Fuji Xerox Print Utility
        "jp.co.fujixerox.prt.PrintUtil.Karin",      // Fuji Xeros Print&Scan (S)
        "com.hp.android.print",                     // HP ePrint
        "com.blackspruce.lpd",                      // Let's Print Droid
        "com.threebirds.notesprint",                // NotesPrint print your notes
        "com.xerox.mobileprint",                    // Print Portal (Xerox)
        "com.zebra.kdu",                            // Print Station (Zebra)
        "net.jsecurity.printbot",                   // PrintBot
        "com.dynamixsoftware.printhand",            // PrintHand Mobile Print
        "com.dynamixsoftware.printhand.premium",    // PrintHand Mobile Print Premium
        "com.sec.print.mobileprint",                // Samsung Mobile Print
        "com.rcreations.send2printer",              // Send 2 Printer
        "com.ivc.starprint",                        // StarPrint
        "com.threebirds.easyviewer",                // WiFi Print
        "com.woosim.android.print",                 // Woosim BT printer
        "com.woosim.bt.app",                        // WoosimPrinter
        "com.woosim.android.btprint",                        // Labor C Woosim Printer Server
        "com.zebra.android.zebrautilities",         // Zebra Utilities
    };

    // Key names received from the BluetoothPrintService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Message types sent from the BluetoothPrintService Handler
    public static final int MESSAGE_DEVICE_NAME = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_READ = 3;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the print services
    private BluetoothPrintService mPrintService = null;
    private WoosimService mWoosim = null;

    private static final String LOG_TAG = "BluetoothPrinter";
    BluetoothDevice mmDevice;



    // following printer models are supported
    // int PRINTER_ARM = 1;    // orange - older model
    // int PRINTER_RX = 3;     // grau - newer

    private int printerModel = 0;           // model PRINTER_RX
    private int printerFontSize = 2;        // font-size: 2
    private String printerAddress = "";     // printer MAC-Adress

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
    }


    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("print".equals(action)) {
            print(args, callbackContext);

            return true;
        }

        if ("connect".equals(action)) {
            try {
                connect(args, callbackContext);
            } catch (Exception ex) {
                System.out.println( "error: " + ex.getMessage());
            }
            return true;
        }

        if ("isServiceAvailable".equals(action)) {
            isServiceAvailable(callbackContext);
            return true;
        }

        if ("list".equals(action)) {
            listBT(callbackContext);
            return true;
        }

        if ("scanDirectory".equals(action)) {
            scanDirectory( args, callbackContext);
        }
        // Returning false results in a "MethodNotFound" error.
        return false;
    }

    private void scanDirectory (final JSONArray args, CallbackContext ctx) {
        final CallbackContext cbContext = ctx;
        String msg = "";
        PluginResult result;

        JSONObject config = args.optJSONObject(0);
        String directory = "";
        try {
            directory = config.optString( "directory", "");
        } catch (Exception ex) {
            directory = "/";
        }

        try {

            rescanFolder( Environment.getExternalStorageDirectory() + directory );



            // var sdcardDBPath = cordova.file.externalRootDirectory + "Download/ParkraumPlus/";
            //Runtime.getRuntime().exec("am broadcast -a android.intent.action.MEDIA_MOUNTED -d file://" + Environment.getExternalStorageDirectory() + "/Download/ParkraumPlus/" );

            /*
            this.cordova.getActivity().sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.parse("file://" + Environment.getExternalStorageDirectory())));

            this.cordova.getActivity().sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/Download/ParkraumPlus/")));

            this.cordova.getActivity().sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.parse("file:///Removable")));
            this.cordova.getActivity().sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.parse("file:///Removable/SD")));
            this.cordova.getActivity().sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.parse("file:///Removable/MicroSD")));
            this.cordova.getActivity().sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.parse("file:///mnt/Removable/MicroSD")));
            this.cordova.getActivity().sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.parse("file:///mnt")));
            this.cordova.getActivity().sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.parse("file:///storage")));
            this.cordova.getActivity().sendBroadcast(new Intent("android.intent.action.MEDIA_MOUNTED", Uri.parse("file:///Removable")));
            */

        } catch (Exception e) {
            e.printStackTrace();
        }

        msg = "scanDirectory Done";
        args.put( msg );

        result = new PluginResult(PluginResult.Status.OK, args);
        ctx.sendPluginResult( result );
    }


    private void rescanFolder(String dest) {
        // Scan files only (not folders);
        File[] files = new File(dest).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });

        String[] paths = new String[files.length];
        for (int co=0; co< files.length; co++)
            paths[co] = files[co].getAbsolutePath();

        MediaScannerConnection.scanFile( this.cordova.getActivity(), paths, null, null);

        // and now recursively scan subfolders
        files = new File(dest).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (int co=0; co<files.length; co++)
            rescanFolder(files[co].getAbsolutePath());
    }



    private void isServiceAvailable (CallbackContext ctx) {
        JSONArray appIds  = this.getInstalledAppIds();
        Boolean available = appIds.length() > 0;
        JSONArray args    = new JSONArray();
        PluginResult result;

        args.put(available);
        args.put(appIds);

        result = new PluginResult(PluginResult.Status.OK, args);

        ctx.sendPluginResult( result );
    }



    private void connect (final JSONArray args, CallbackContext ctx) {
        //JSONArray args    = new JSONArray();
        String msg = "";
        PluginResult result;


        JSONObject connectConfig = args.optJSONObject(0);

        this.printerModel = connectConfig.optInt("model", this.printerModel);
        this.printerFontSize = connectConfig.optInt("fontsize", this.printerFontSize);
        this.printerAddress = connectConfig.optString("address", "00:00:00:00:00:00");

        // try to get the printer
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            System.out.println( "+++ BLUETOOTH NOT SUPPORTED +++");
            return;
        } else {
            System.out.println( "+++ BLUETOOTH SUPPORTED +++");
        }




        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.cordova.getActivity().startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mPrintService == null) setupPrint();
        }



        if (mPrintService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mPrintService.getState() == BluetoothPrintService.STATE_NONE) {
                // Start the Bluetooth print services
                System.out.println(" ++++ PRINT SERVICE START +++");
                mPrintService.start();
                System.out.println( "start print service on address: " + this.printerAddress );
                this.connectWoosimPrinter( this.printerAddress );

            }
        }

        msg = "Device: " + mConnectedDeviceName;

        args.put(msg);


        result = new PluginResult(PluginResult.Status.OK, args);
        ctx.sendPluginResult( result );
    }


    private void setupPrint() {
        try {

            /* -- mario: dies muss man einschalten, um einen neuen drucker zu selecten -- INFO laborc BUG BUG BUG
            Intent serverIntent = null;
            serverIntent = new Intent(this.cordova.getActivity(), DeviceListActivity.class);
            this.cordova.setActivityResultCallback( this);
            this.cordova.getActivity().startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            */

            // connect to the woosim printer
            //this.connectWoosimPrinter( "00:15:0E:E2:67:A1" );


        } catch (Exception e) {
            System.out.println( e.toString() );
        }

        try {
            // Initialize the BluetoothPrintService to perform bluetooth connections
            mPrintService = new BluetoothPrintService(this.cordova.getActivity(), mHandler);
            mWoosim = new WoosimService(mHandler);
        } catch (Exception ex ) {
            System.out.println( ex.toString() );
        }
    }

    // The Handler that gets information back from the BluetoothPrintService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    //Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    System.out.println( "Connected to " + mConnectedDeviceName );
                    break;
                case MESSAGE_TOAST:
                    // Toast.makeText(getApplicationContext(), msg.getData().getInt(TOAST), Toast.LENGTH_SHORT).show();
                    System.out.println("what message" + msg.getData().toString());
                    break;
                case MESSAGE_READ:
                    mWoosim.processRcvData((byte[])msg.obj, msg.arg1);
                    break;
                case WoosimService.MESSAGE_PRINTER:
                    switch (msg.arg1) {
                        case WoosimService.MSR:
                            if (msg.arg2 == 0) {
                                // Toast.makeText(getApplicationContext(), "MSR reading failure", Toast.LENGTH_SHORT).show();
                                System.out.println("MSR reading failure");
                            } else {
                                byte[][] track = (byte[][])msg.obj;
                            }
                            break;
                    }
                    break;
            }
        }
    };




    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a print
                    setupPrint();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    System.out.println("BT not enabled - finish");
                    return;
                }
        }
    }



    private void connectWoosimPrinter( String address) {
        // Get the device MAC address
        try {
            //address = "00:15:0E:E2:67:A1"; // printer grau labor c
            //address = "00:15:0E:E2:67:A0"; // BUG BUG BUG --> woosim printer adresse soll nicht absolut sein -- mario laborc // printer grau braunau
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            mPrintService.connect(device, true);
        } catch (Exception ex) {
            System.out.println( ex.toString());
        }
    }


    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString("");
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
            //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            System.out.println( "+++ PRINTER NOT CONNECTED +++");
            return;
        }

        // Check that there's actually something to send
        if (data.length > 0)
            mPrintService.write(data);
    }



    public void print( String text2print ) {

        byte[] text = text2print.getBytes();
        if (text.length == 0) return;


        // some settings on the printer model
        int PRINTER_TYPE = this.printerModel;
        int font_width = this.printerFontSize;


        // ARM
        int CODE_TABLE_Western_Europe_Latin2 = 2;
        int CODE_TABLE_Western_Europe_Latin9 = 13;  // CT_ISO8859_15

        // RXPRINTER_RX
        // CODE_TABLE_Western_Europe_Latin2 = 2;
        // CODE_TABLE_Western_Europe_Latin9 = 13;  // CT_ISO8859_15
        int CT_CP858 = 15; // Western Europe with Euro Sign --> only RX printer
        int CT_WIN1252 = 14; // Windows 1252 for Latin
        int CT_WIN1250 = 18; // Windows 1250 Central and Eastern Europe
        int CODE_TABLE = CODE_TABLE_Western_Europe_Latin9;


        // tbis has to be set - at the moment
        PRINTER_TYPE = PRINTER_TYPE;


        int PRINTER_ARM = 1;    // orange-
        int PRINTER_RX = 3;     // grau alle

        if (PRINTER_TYPE == PRINTER_ARM)
        {
            // ARM CPU
            // old model => orange
            CODE_TABLE = CODE_TABLE_Western_Europe_Latin9;
        }
        else
        {
            // RX CPU
            // new model => gray
            CODE_TABLE = CT_WIN1250;
        }


        byte[] myBText = WoosimCmd.getTTFcode( 2, 1, text2print);
        ByteArrayOutputStream myBuffer = new ByteArrayOutputStream(2048);
        byte[] cmd_ct = WoosimCmd.setCodeTable( PRINTER_TYPE, CODE_TABLE, 1);
        byte[] cmd_ts = WoosimCmd.setTextStyle( false, false, false, font_width, 1);
        byte[] cmd_ta = WoosimCmd.setTextAlign( WoosimCmd.ALIGN_LEFT );
        myBuffer.write( cmd_ct, 0, cmd_ct.length);
        myBuffer.write( cmd_ts, 0, cmd_ts.length);
        myBuffer.write( cmd_ta, 0, cmd_ta.length);
        //myBuffer.append(WoosimCmd.selectTTF("truetype.ttf"), 0, WoosimCmd.selectTTF("truetype.ttf").length);
        myBuffer.write( myBText, 0, myBText.length);
        sendData(WoosimCmd.initPrinter());
        sendData(myBuffer.toByteArray());




        //----- end print
        boolean print_debug = false;
        if (print_debug) {
            // just a printer test with special chars of the german lanuage

            String myText = "1 öäüß \n";
            myBText = WoosimCmd.getTTFcode(2, 1, myText);
            myBuffer = new ByteArrayOutputStream(4096);
            cmd_ct = WoosimCmd.setCodeTable(PRINTER_TYPE, CODE_TABLE_Western_Europe_Latin2, 1);
            cmd_ts = WoosimCmd.setTextStyle(false, false, false, 2, 1);
            cmd_ta = WoosimCmd.setTextAlign(WoosimCmd.ALIGN_LEFT);
            myBuffer.write(cmd_ct, 0, cmd_ct.length);
            myBuffer.write(cmd_ts, 0, cmd_ts.length);
            myBuffer.write(cmd_ta, 0, cmd_ta.length);
            //myBuffer.append(WoosimCmd.selectTTF("truetype.ttf"), 0, WoosimCmd.selectTTF("truetype.ttf").length);
            myBuffer.write(myBText, 0, myBText.length);
            sendData(WoosimCmd.initPrinter());
            sendData(myBuffer.toByteArray());

            myText = "2 öäüß \n";
            myBText = WoosimCmd.getTTFcode(2, 1, myText);
            myBuffer = new ByteArrayOutputStream(2048);
            cmd_ct = WoosimCmd.setCodeTable(PRINTER_TYPE, CODE_TABLE_Western_Europe_Latin9, 1);
            cmd_ts = WoosimCmd.setTextStyle(false, false, false, 2, 1);
            cmd_ta = WoosimCmd.setTextAlign(WoosimCmd.ALIGN_LEFT);
            myBuffer.write(cmd_ct, 0, cmd_ct.length);
            myBuffer.write(cmd_ts, 0, cmd_ts.length);
            myBuffer.write(cmd_ta, 0, cmd_ta.length);
            //myBuffer.append(WoosimCmd.selectTTF("truetype.ttf"), 0, WoosimCmd.selectTTF("truetype.ttf").length);
            myBuffer.write(myBText, 0, myBText.length);
            sendData(WoosimCmd.initPrinter());
            sendData(myBuffer.toByteArray());

            myText = "3 öäüß \n";
            myBText = WoosimCmd.getTTFcode(2, 1, myText);
            myBuffer = new ByteArrayOutputStream(2048);
            cmd_ct = WoosimCmd.setCodeTable(PRINTER_TYPE, CT_CP858, 1);
            cmd_ts = WoosimCmd.setTextStyle(false, false, false, 2, 1);
            cmd_ta = WoosimCmd.setTextAlign(WoosimCmd.ALIGN_LEFT);
            myBuffer.write(cmd_ct, 0, cmd_ct.length);
            myBuffer.write(cmd_ts, 0, cmd_ts.length);
            myBuffer.write(cmd_ta, 0, cmd_ta.length);
            //myBuffer.append(WoosimCmd.selectTTF("truetype.ttf"), 0, WoosimCmd.selectTTF("truetype.ttf").length);
            myBuffer.write(myBText, 0, myBText.length);
            sendData(WoosimCmd.initPrinter());
            sendData(myBuffer.toByteArray());

            myText = "4 öäüß \n";
            myBuffer = new ByteArrayOutputStream(2048);
            cmd_ct = WoosimCmd.setCodeTable(PRINTER_TYPE, CODE_TABLE_Western_Europe_Latin9, 1);
            cmd_ts = WoosimCmd.setTextStyle(false, false, false, 2, 1);
            cmd_ta = WoosimCmd.setTextAlign(WoosimCmd.ALIGN_LEFT);
            myBuffer.write(cmd_ct, 0, cmd_ct.length);
            myBuffer.write(cmd_ts, 0, cmd_ts.length);
            myBuffer.write(cmd_ta, 0, cmd_ta.length);
            //myBuffer.append(WoosimCmd.selectTTF("truetype.ttf"), 0, WoosimCmd.selectTTF("truetype.ttf").length);
            myBuffer.write(myBText, 0, myBText.length);
            sendData(WoosimCmd.initPrinter());
            sendData(myBuffer.toByteArray());

            myText = "5 öäüß \n";
            myBText = WoosimCmd.getTTFcode(2, 1, myText);
            myBuffer = new ByteArrayOutputStream(2048);
            cmd_ct = WoosimCmd.setCodeTable(PRINTER_TYPE, CT_WIN1250, 1);
            cmd_ts = WoosimCmd.setTextStyle(false, false, false, 2, 1);
            cmd_ta = WoosimCmd.setTextAlign(WoosimCmd.ALIGN_LEFT);
            myBuffer.write(cmd_ct, 0, cmd_ct.length);
            myBuffer.write(cmd_ts, 0, cmd_ts.length);
            myBuffer.write(cmd_ta, 0, cmd_ta.length);
            //myBuffer.append(WoosimCmd.selectTTF("truetype.ttf"), 0, WoosimCmd.selectTTF("truetype.ttf").length);
            myBuffer.write(myBText, 0, myBText.length);
            sendData(WoosimCmd.initPrinter());
            sendData(myBuffer.toByteArray());

            myText = "6 öäüß \n";
            myBText = WoosimCmd.getTTFcode(2, 1, myText);
            myBuffer = new ByteArrayOutputStream(2048);
            cmd_ct = WoosimCmd.setCodeTable(PRINTER_TYPE, CT_WIN1252, 1);
            cmd_ts = WoosimCmd.setTextStyle(false, false, false, 2, 1);
            cmd_ta = WoosimCmd.setTextAlign(WoosimCmd.ALIGN_LEFT);
            myBuffer.write(cmd_ct, 0, cmd_ct.length);
            myBuffer.write(cmd_ts, 0, cmd_ts.length);
            myBuffer.write(cmd_ta, 0, cmd_ta.length);
            //myBuffer.append(WoosimCmd.selectTTF("truetype.ttf"), 0, WoosimCmd.selectTTF("truetype.ttf").length);
            myBuffer.write(myBText, 0, myBText.length);
            sendData(WoosimCmd.initPrinter());
            sendData(myBuffer.toByteArray());

        }



    }


    private void print (final JSONArray args, CallbackContext ctx) {
        final Printer self = this;

        String msg = "";
        PluginResult result;

        // this.ctx = ctx;

        JSONObject platformConfig = args.optJSONObject(1);
        String content    = args.optString(0, "<html></html>");

        print( content );

        result = new PluginResult(PluginResult.Status.OK, args);
        ctx.sendPluginResult( result );

    }

    private String getPrintAppId (JSONObject platformConfig) {
        String appId = platformConfig.optString("appId", null);

        if (appId != null) {
            return (this.isAppInstalled(appId)) ? appId : null;
        } else {
            return this.getFirstInstalledAppId();
        }
    }

    private String getJSONValue (JSONObject platformConfig,String key) {
        String value = platformConfig.optString(key, null);
		return value;
    }

    private Intent getPrintController (String appId) {
        String intentId = "android.intent.action.SEND";

        if (appId.equals("com.rcreations.send2printer")) {
            intentId = "com.rcreations.send2printer.print";
        } else if (appId.equals("com.dynamixsoftware.printershare")) {
            intentId = "android.intent.action.VIEW";
        } else if (appId.equals("com.hp.android.print")) {
            intentId = "org.androidprinting.intent.action.PRINT";
        }

        Intent intent = new Intent(intentId);
        if (appId != null)
            intent.setPackage(appId);

        return intent;



    }

    private void adjustSettingsForPrintController (Intent intent,String mimeType) {
        if(mimeType == null){
			mimeType = "image/png";
		}
        String appId    = intent.getPackage();

        // Check for special cases that can receive HTML
        //if (appId.equals("com.rcreations.send2printer") || appId.equals("com.dynamixsoftware.printershare")) {
          //  mimeType = "text/html";
		   intent.setType(mimeType);
        //}

     //   intent.setType(mimeType);
    }



    private void startPrinterApp (Intent intent) {

        try {
            cordova.startActivityForResult(this, intent, 0);
        } catch (Exception e) {

            System.out.println( e.getStackTrace() );
        }
    }

    private boolean isAppInstalled (String appId) {
        PackageManager pm = cordova.getActivity().getPackageManager();

        try {
            PackageInfo pi = pm.getPackageInfo(appId, 0);

            if (pi != null){
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {}

        return false;
    }

    private JSONArray getInstalledAppIds () {
        JSONArray appIds  = new JSONArray();

        for (int i = 0; i < printAppIds.length; i++) {
            String appId        = printAppIds[i];
            Boolean isInstalled = this.isAppInstalled(appId);

            if (isInstalled){
                appIds.put(appId);
            }
        }

        return appIds;
    }

    private String getFirstInstalledAppId () {
        for (int i = 0; i < printAppIds.length; i++) {
            String appId        = printAppIds[i];
            Boolean isInstalled = this.isAppInstalled(appId);

            if (isInstalled){
                return appId;
            }
        }

        return null;
    }



    void listBT(CallbackContext callbackContext) {
        BluetoothAdapter mBluetoothAdapter = null;
        String errMsg = null;
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                errMsg = "No bluetooth adapter available";
                Log.e(LOG_TAG, errMsg);
                callbackContext.error(errMsg);
                return;
            }
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
            }
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                JSONArray json = new JSONArray();
                for (BluetoothDevice device : pairedDevices) {
					Hashtable map = new Hashtable();
				    map.put("type", device.getType());
					map.put("address", device.getAddress());
					map.put("name", device.getName());
					JSONObject jObj = new JSONObject(map);
                    //json.put(device.getName());
                    json.put( jObj );
                }
                callbackContext.success(json);
            } else {
                callbackContext.error("No Bluetooth Device Found");
            }
//			Log.d(LOG_TAG, "Bluetooth Device Found: " + mmDevice.getName());
        } catch (Exception e) {
            errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
    }

    // This will find a bluetooth printer device
    boolean findBT(CallbackContext callbackContext, String name) {
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(LOG_TAG, "No bluetooth adapter available");
            }
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    // MP300 is the name of the bluetooth printer device
                    if (device.getName().equalsIgnoreCase(name)) {
                        mmDevice = device;
                        return true;
                    }
                }
            }
            Log.d(LOG_TAG, "Bluetooth Device Found: " + mmDevice.getName());
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }





}
