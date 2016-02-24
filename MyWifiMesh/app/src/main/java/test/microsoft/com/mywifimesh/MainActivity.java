package test.microsoft.com.mywifimesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.Formatter;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;



public class MainActivity extends ActionBarActivity {

    public static final String SERVICE_TYPE = "_wdm_p2p._tcp";

    // make static so we can recover on activity recreate.
    public static String logOutput = "";
    public static SpannableStringBuilder logOutputSB = new SpannableStringBuilder();

    MainActivity that = this;

    MyTextSpeech mySpeech = null;

    MainBCReceiver mBRReceiver;
    private IntentFilter filter;
    TextView debugdataBox;

    private int mInterval = 1000; // 1 second by default, can be changed later
    private Handler timeHandler;
    private int timeCounter = 0;
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            // call function to update timer
            timeCounter = timeCounter + 1;
            ((TextView) findViewById(R.id.TimeBox)).setText("T: " + timeCounter);
            timeHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    WifiServiceSearcher    mWifiServiceSearcher = null;
    WifiAccessPoint        mWifiAccessPoint = null;
    WifiConnection         mWifiConnection = null;
    Boolean serviceRunning = false;

    //change me  to be dynamic!!
    public String CLIENT_PORT_INSTANCE = "38765";
    public String SERVICE_PORT_INSTANCE = "38765";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;

    GroupOwnerSocketHandler  groupSocket = null;
    ClientSocketHandler clientSocket = null;
    ChatManager chat = null;
    Handler myHandler  = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:

                    byte[] readBuf = (byte[]) msg.obj;

                    String  readMessage = new String(readBuf, 0, msg.arg1);

                    print_line("","Got message: " + readMessage);

                    mySpeech.speak(readMessage);
                    break;

                case MY_HANDLE:
                    Object obj = msg.obj;
                    chat = (ChatManager) obj;

                    String helloBuffer = "Hello There from " +  chat.getSide() + " :" + Build.VERSION.SDK_INT;

                    chat.write(helloBuffer.getBytes());
                    print_line("","Wrote message: " + helloBuffer);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a single instance we can share for errors
        WifiP2pHelper.setDebugLocalBroadcaster(LocalBroadcastManager.getInstance(this.getApplicationContext()));

        setContentView(R.layout.activity_main);

        mySpeech = new MyTextSpeech(this);

        debugdataBox = ((TextView)findViewById(R.id.debugdataBox));
        if (debugdataBox != null)
        {
            debugdataBox.setText(logOutputSB);
        }

        Button showIPButton = (Button) findViewById(R.id.button3);
        showIPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyP2PHelper.printLocalIpAddresses(that);
            }
        });

        Button clearButton = (Button) findViewById(R.id.button2);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TextView) findViewById(R.id.debugdataBox)).setText("");
            }
        });

        Button toggleButton = (Button) findViewById(R.id.buttonToggle);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serviceRunning){
                    serviceRunning = false;
                    if(mWifiAccessPoint != null){
                        mWifiAccessPoint.Stop();
                        mWifiAccessPoint = null;
                    }

                    if(mWifiServiceSearcher != null){
                        mWifiServiceSearcher.Stop();
                        mWifiServiceSearcher = null;
                    }

                    if(mWifiConnection != null) {
                        mWifiConnection.Stop();
                        mWifiConnection = null;
                    }
                    print_line("","Stopped");
                }else{
                    serviceRunning = true;
                    print_line("","Started");

                    mWifiAccessPoint = new WifiAccessPoint(that);
                    mWifiAccessPoint.Start();

                    mWifiServiceSearcher = new WifiServiceSearcher(that);
                    mWifiServiceSearcher.Start();
                }
            }
        });
        mBRReceiver = new MainBCReceiver();
        filter = new IntentFilter();
        filter.addAction(WifiAccessPoint.DSS_WIFIAP_VALUES);
        filter.addAction(WifiAccessPoint.DSS_WIFIAP_SERVERADDRESS);
        filter.addAction(WifiServiceSearcher.DSS_WIFISS_PEERAPINFO);
        filter.addAction(WifiServiceSearcher.DSS_WIFISS_PEERCOUNT);
        filter.addAction(WifiServiceSearcher.DSS_WIFISS_VALUES);
        filter.addAction(WifiConnection.DSS_WIFICON_VALUES);
        filter.addAction(WifiConnection.DSS_WIFICON_STATUSVAL);
        filter.addAction(WifiConnection.DSS_WIFICON_SERVERADDRESS);
        filter.addAction(ClientSocketHandler.DSS_CLIENT_VALUES);
        filter.addAction(GroupOwnerSocketHandler.DSS_GROUP_VALUES);
        filter.addAction(WifiP2pHelper.GENERAL_CLIENT_MESSAGE);

        LocalBroadcastManager.getInstance(this).registerReceiver((mBRReceiver), filter);

        try{
            groupSocket = new GroupOwnerSocketHandler(myHandler,Integer.parseInt(SERVICE_PORT_INSTANCE),this);
            groupSocket.start();
            print_line("","Group socketserver started.");
        }catch (Exception e){
            print_line_error("", "groupseocket error, :" + e.toString());
        }

        timeHandler  = new Handler();
        mStatusChecker.run();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_wifi_mesh, menu);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mWifiConnection != null) {
            mWifiConnection.Stop();
            mWifiConnection = null;
        }
        if(mWifiAccessPoint != null){
            mWifiAccessPoint.Stop();
            mWifiAccessPoint = null;
        }

        if(mWifiServiceSearcher != null){
            mWifiServiceSearcher.Stop();
            mWifiServiceSearcher = null;
        }

        timeHandler.removeCallbacks(mStatusChecker);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBRReceiver);
        if (mySpeech != null)
        {
            mySpeech.stop();;
        }
    }


    public void print_line(String who,String line) {
        timeCounter = 0;
        logOutput += who + ": " + line + "\n";
        logOutputSB.append(who + ": " + line + "\n");

        if (debugdataBox != null) {
            debugdataBox.append(who + ": " + line + "\n");
        }
    }

    public void print_line_error(String who, String line) {
        timeCounter = 0;
        logOutput += who + " : " + line + "\n";

        SpannableString newContentSpannableString = new SpannableString(line);
        newContentSpannableString.setSpan(new ForegroundColorSpan(Color.RED), 0, line.length(), 0);

        logOutputSB.append(who + ": ");
        logOutputSB.append(newContentSpannableString);
        logOutputSB.append("\n");

        if (debugdataBox != null) {
            // debugdataBox.append(who + " : " + line + "\n");
            debugdataBox.append(who + ": ");
            debugdataBox.append(newContentSpannableString);
            debugdataBox.append("\n");
        }
    }


    private class MainBCReceiver extends BroadcastReceiver {

        public void routePrintMessage(String outTag, String s, boolean isError)
        {
            if (isError)
                print_line_error(outTag, s);
            else
                print_line(outTag, s);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            boolean isError = intent.getBooleanExtra(WifiP2pHelper.EXTRA_ERROR_INDICATOR, false);

            if (WifiAccessPoint.DSS_WIFIAP_VALUES.equals(action)) {
                String s = intent.getStringExtra(WifiAccessPoint.DSS_WIFIAP_MESSAGE);
                routePrintMessage("_AP", s, isError);

            }else if (WifiAccessPoint.DSS_WIFIAP_SERVERADDRESS.equals(action)) {
                InetAddress address = (InetAddress)intent.getSerializableExtra(WifiAccessPoint.DSS_WIFIAP_INETADDRESS);
                print_line("AP", "inet address" + address.getHostAddress());

            }else if (WifiServiceSearcher.DSS_WIFISS_VALUES.equals(action)) {
                String s = intent.getStringExtra(WifiServiceSearcher.DSS_WIFISS_MESSAGE);
                routePrintMessage("_SS", s, isError);
            }else if (WifiServiceSearcher.DSS_WIFISS_PEERCOUNT.equals(action)) {
                int s = intent.getIntExtra(WifiServiceSearcher.DSS_WIFISS_COUNT, -1);
                print_line("SS", "found " + s + " peers");
                mySpeech.speak(s+ " peers discovered.");

            }else if (WifiServiceSearcher.DSS_WIFISS_PEERAPINFO.equals(action)) {
                String s = intent.getStringExtra(WifiServiceSearcher.DSS_WIFISS_INFOTEXT);

                String[] separated = s.split(":");
                print_line("SS", "found SSID:" + separated[1] + ", pwd:"  + separated[2]+ "IP: " + separated[3]);

                if(mWifiConnection == null) {
                    if(mWifiAccessPoint != null){
                        mWifiAccessPoint.Stop();
                        mWifiAccessPoint = null;
                    }
                    if(mWifiServiceSearcher != null){
                        mWifiServiceSearcher.Stop();
                        mWifiServiceSearcher = null;
                    }

                    final String networkSSID = separated[1];
                    final String networkPass = separated[2];
                    final String ipAddress   = separated[3];

                    mWifiConnection = new WifiConnection(that,networkSSID,networkPass);
                    mWifiConnection.SetInetAddress(ipAddress);
                    mySpeech.speak("found accesspoint");
                }
            }else if (WifiConnection.DSS_WIFICON_VALUES.equals(action)) {
                String s = intent.getStringExtra(WifiConnection.DSS_WIFICON_MESSAGE);
                print_line("CON", s);

            }else if (WifiConnection.DSS_WIFICON_SERVERADDRESS.equals(action)) {
                int addr = intent.getIntExtra(WifiConnection.DSS_WIFICON_INETADDRESS, -1);
                print_line("COM", "IP" + Formatter.formatIpAddress(addr));

                if(clientSocket == null &&  mWifiConnection != null) {
                    String IpToConnect = mWifiConnection.GetInetAddress();
                    print_line("","Starting client socket conenction to : " + IpToConnect);
                    clientSocket = new ClientSocketHandler(myHandler,IpToConnect, Integer.parseInt(CLIENT_PORT_INSTANCE), that);
                    clientSocket.start();
                }
            }else if (WifiConnection.DSS_WIFICON_STATUSVAL.equals(action)) {
                int status = intent.getIntExtra(WifiConnection.DSS_WIFICON_CONSTATUS, -1);

                String conStatus = "";
                if(status == WifiConnection.ConectionStateNONE) {
                    conStatus = "NONE";
                }else if(status == WifiConnection.ConectionStatePreConnecting) {
                    conStatus = "PreConnecting";
                }else if(status == WifiConnection.ConectionStateConnecting) {
                    conStatus = "Connecting";
                    mySpeech.speak("Accesspoint connected");
                }else if(status == WifiConnection.ConectionStateConnected) {
                    conStatus = "Connected";
                }else if(status == WifiConnection.ConectionStateDisconnected) {
                    conStatus = "Disconnected";
                    mySpeech.speak("Accesspoint Disconnected");
                    if(mWifiConnection != null) {
                        mWifiConnection.Stop();
                        mWifiConnection = null;
                        // should stop etc.
                        clientSocket = null;
                    }
                    // make sure services are re-started
                    if(mWifiAccessPoint != null){
                        mWifiAccessPoint.Stop();
                        mWifiAccessPoint = null;
                    }
                    mWifiAccessPoint = new WifiAccessPoint(that);
                    mWifiAccessPoint.Start();

                    if(mWifiServiceSearcher != null){
                        mWifiServiceSearcher.Stop();
                        mWifiServiceSearcher = null;
                    }

                    mWifiServiceSearcher = new WifiServiceSearcher(that);
                    mWifiServiceSearcher.Start();
                }

                print_line("COM", "Status " + conStatus);
            }else if (ClientSocketHandler.DSS_CLIENT_VALUES.equals(action)) {
                String s = intent.getStringExtra(ClientSocketHandler.DSS_CLIENT_MESSAGE);
                routePrintMessage("_Client_", s, isError);
            }else if (WifiP2pHelper.GENERAL_CLIENT_MESSAGE.equals(action)) {
                String s = intent.getStringExtra(WifiP2pHelper.GENERAL_MESSAGE_PAYLOAD);
                routePrintMessage("_GENERAL_", s, isError);
            }else if (GroupOwnerSocketHandler.DSS_GROUP_VALUES.equals(action)) {
                String s = intent.getStringExtra(GroupOwnerSocketHandler.DSS_GROUP_MESSAGE);
                print_line("Group", s);
            }
        }
    }
}
