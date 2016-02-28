package test.microsoft.com.mywifimesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;


public class MainActivity extends ActionBarActivity {

    public static final String SERVICE_TYPE = "_wdm_p2p._tcp";

    // make static so we can recover on activity recreate.
    public static String logOutput = "";
    public static SpannableStringBuilder logOutputSB = new SpannableStringBuilder();
    public static int COLOR_DARKGREEN0 = Color.parseColor("#006400");

    MyTextSpeech mySpeech = null;

    MainBCReceiver mBRReceiver;
    private IntentFilter filter;
    TextView debugdataBox;
    TextView peerInterfaceInfo0;
    TextView meshStateInfo0;
    TextView meshStateInfo1;
    TextView internetConnectInfo0;
    TextView TimeBox;

    private int mInterval = 1000; // 1 second by default, can be changed later
    private Handler timeHandler;
    private int timeCounter = 0;
    private static final int INTERNET_TEST_ISSUE_MAX = 50;
    private long internetTestIssuedMostRecentWhen = 0L;
    private long internetTestIssuedFirstWhen = 0L;
    private int  internetTestIssuedCount = 0;
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            // call function to update timer
            // Design is such that timeCounter is set to zero after each screen update
            if (timeCounter == 0)
            {
                Log.d("Main", "timeCounter run " + timeCounter);
                MyP2PHelper.getLikelyTargetInterface(MainActivity.this, true /* Force rebuild */);
            }
            timeCounter = timeCounter + 1;
            TimeBox.setText("T: " + timeCounter);

            drawNetworkInformation();

            // We shouldn't hammer on the public website forever
            // ToDo: have a Settings checkbox to set higher?
            if (internetTestIssuedCount < INTERNET_TEST_ISSUE_MAX)
            {
                // shut down test after certain period of inactivity. Reminder: Adding any output resets timer to 0.
                if (System.currentTimeMillis() - internetTestIssuedMostRecentWhen > (12L * 1000L)) {
                    internetTestIssuedCount++;
                    if (internetTestIssuedCount == INTERNET_TEST_ISSUE_MAX)
                    {
                        print_line_error("MA", "This will be the final website test issued, limit is " + INTERNET_TEST_ISSUE_MAX + " (menu can reset to 0)");
                    }
                    internetTestIssuedMostRecentWhen = System.currentTimeMillis();
                    Log.d("Main", "website test issuing: " + internetTestIssuedCount);
                    // For refeernce, the drawNetworkInformation() method on screen if less than 30 seconds since result.
                    meshManager.sendMessageToInternetTestHandler(InternetTestHandler.HANDLER_MESSAGE_INTERNET_WEBSITE_TEST_A);
                }
            }

            // start self again
            timeHandler.postDelayed(mStatusChecker, mInterval);
        }
    };


    public static MeshManager meshManager;

    //change me  to be dynamic!!
    public static final int SOCKET_SERVER_PORT = 38765;
    public int CLIENT_PORT_INSTANCE  = SOCKET_SERVER_PORT;
    public int SERVICE_PORT_INSTANCE = SOCKET_SERVER_PORT;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a single instance we can share for errors
        WifiP2pHelper.setDebugLocalBroadcaster(LocalBroadcastManager.getInstance(this.getApplicationContext()));

        setContentView(R.layout.mywifimesh_activity_main);

        // Ru in App context so we can destroy activity and still have some background thread activity
        if (meshManager == null)
        {
            meshManager = new MeshManager();
            meshManager.setupMeshManager(getApplicationContext());
        }

        EventBus.getDefault().register(this);

        mySpeech = new MyTextSpeech(this);

        debugdataBox = (TextView)findViewById(R.id.debugdataBox);
        if (debugdataBox != null)
        {
            debugdataBox.setText(logOutputSB);
        }
        peerInterfaceInfo0 = (TextView)findViewById(R.id.peerInterfaceInfo0);
        meshStateInfo0 = (TextView)findViewById(R.id.meshStateInfo0);
        meshStateInfo1 = (TextView)findViewById(R.id.meshStateInfo1);
        internetConnectInfo0 = (TextView)findViewById(R.id.internetConnectInfo0);
        TimeBox = (TextView) findViewById(R.id.TimeBox);

        Button showIPButton = (Button) findViewById(R.id.buttonIpAddress);
        showIPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyP2PHelper.printLocalIpAddresses(MainActivity.this);
            }
        });

        Button clearButton = (Button) findViewById(R.id.buttonClear);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TextView) findViewById(R.id.debugdataBox)).setText("");
                logOutput = "";
                logOutputSB.clear();
            }
        });

        Button toggleButton = (Button) findViewById(R.id.buttonToggle);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                meshManager.sendMessageToHandler(MeshManager.HANDLER_MESSAGE_ACT_ON_TOGGLE_CLICK);
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

        int serverPort = SERVICE_PORT_INSTANCE;
        meshManager.sendMessageToHandler(MeshManager.HANDLER_MESSAGE_ACT_ON_GROUP_SOCKET_CREATE, serverPort /* arg1 */);

        timeHandler  = new Handler();
        mStatusChecker.run();
    }


    // Called in Android UI's main thread
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UserInterfaceOutputBox outputBox)
    {
        switch (outputBox.messageType)
        {
            case UserInterfaceOutputBox.MESSAGE_TYPE_NORMAL:    // normal
                print_line(outputBox.whoFrom, outputBox.outputMessage);
                break;
            case UserInterfaceOutputBox.MESSAGE_TYPE_ERROR:    // error
                print_line_error(outputBox.whoFrom, outputBox.outputMessage);
                break;
            case UserInterfaceOutputBox.MESSAGE_TYPE_SPEECH:
                mySpeech.speak(outputBox.outputMessage);
                break;
        }
    }


    public void drawNetworkInformation()
    {
        NetworkInterface peerInterface = MyP2PHelper.getLikelyTargetInterface(MainActivity.this, false /* Do not force rebuild */);
        if (peerInterface == null)
        {
            peerInterfaceInfo0.setText("p2p-p2p0 interface: not found");
        }
        else
        {
            SpannableStringBuilder outputBuilder = TextStyleHelper.createBuilderWithContent(peerInterface.getName() + "/" + peerInterface.getDisplayName(), Color.RED);
            try {
                if (peerInterface.isUp()) {
                    String evenMoreInfo0 = " [UP]";
                    outputBuilder = TextStyleHelper.appendSpanWhiteBackground(outputBuilder, evenMoreInfo0, Color.BLUE);
                }
            }
            catch (IOException e0)
            {
                // do nothing.
            }
            outputBuilder.append(":");
            for(InetAddress addr : Collections.list(peerInterface.getInetAddresses())) {
                String interfaceAddress = MyP2PHelper.ipAddressToString(addr);
                outputBuilder.append("\n");
                outputBuilder.append(interfaceAddress);
            }
            peerInterfaceInfo0.setText(outputBuilder);
        }
        if (MeshManager.getAccessPointFoundWhen() > 0L)
        {
            meshStateInfo0.setVisibility(View.VISIBLE);
        }

        MeshState meshState = MeshManager.getMeshState();
        meshStateInfo1.setVisibility(View.VISIBLE);
        SpannableStringBuilder outStateBuilder = new SpannableStringBuilder();
        if (meshState.chatConnected);
        {
            outStateBuilder.append("Chat ");
            outStateBuilder.append(TextStyleHelper.createSpannableStringWhiteBackground("ON", COLOR_DARKGREEN0));
        }
        if (meshState.peersFoundCount > 0)
        {
            outStateBuilder.append(" Peers: ");
            outStateBuilder.append(TextStyleHelper.createSpannableString(meshState.peersFoundCount + "", COLOR_DARKGREEN0));
        }
        if (meshState.chatLastGoodWriteWhen > 0L)
        {
            long chatLastGoodWriteDiff = System.currentTimeMillis() - meshState.chatLastGoodWriteWhen;
            if (meshState.chatLastFailedWriteWhen > meshState.chatLastGoodWriteWhen)
            {
                // make it red to show that we have some problems
                outStateBuilder.append(TextStyleHelper.createSpannableStringWhiteBackground(" Chat W: " + chatLastGoodWriteDiff, Color.RED));
            }
            else {
                outStateBuilder.append(" Chat W: " + chatLastGoodWriteDiff);
            }

            if (meshState.chatConnected) {
                // enable the menu
                menuItemSendSocket.setEnabled(true);
            }
        }
        if (meshState.peer2PeerOnChannelDisconnectCount > 0)
        {
            outStateBuilder.append(" P2P_CH_DIS " + meshState.peer2PeerOnChannelDisconnectCount);
        }
        if (meshState.netWifiConnectedCount > 0)
        {
            outStateBuilder.append(" N_WiFi_CON " + meshState.netWifiConnectedCount);
        }
        if (meshState.peer2PeerGroupClientCount > 0)
        {
            outStateBuilder.append(" P2P_G_CLIENTS " + meshState.peer2PeerGroupClientCount);
        }
        if (meshState.chatServerSocketAcceptCount > 0)
        {
            outStateBuilder.append(" C_S_A " + meshState.chatServerSocketAcceptCount);
        }
        if (meshState.netWifiGotInfo != null)
        {
            outStateBuilder.append(" N_WiFi_Got " + meshState.netWifiGotInfo.getSSID() + " " + Formatter.formatIpAddress(meshState.netWifiGotInfo.getIpAddress()));
        }
        if (meshState.peerDiscoveryLogA.length() > 0)
        {
            outStateBuilder.append(" PD_LA: " + meshState.peerDiscoveryLogA);
        }
        if (meshState.peer2PeerDiscoverPeersSuccessWhen != 0L)
        {
            outStateBuilder.append(" P2PPD!");
        }

        meshStateInfo1.setText(outStateBuilder);

        internetConnectInfo0.setVisibility(View.GONE);
        if (MeshManager.getMeshState().internetConnectionTestWebsiteAGood)
        {
            long elapsedTimeSinceTest = System.currentTimeMillis() - MeshManager.getMeshState().internetConnectionTestWebsiteAGoodWhen;
            if (elapsedTimeSinceTest < (30L * 1000L))
            {
                internetConnectInfo0.setVisibility(View.VISIBLE);
                internetConnectInfo0.setText("Website fetch: " + MeshManager.getMeshState().internetConnectionTestWebsiteAcontentMostRecent + " {" + elapsedTimeSinceTest + "}");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_wifi_mesh, menu);
        menuItemSendSocket = menu.findItem(R.id.action_send_socket);
        return true;
    }

    MenuItem menuItemSendSocket = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        switch (item.getItemId()) {
            case R.id.action_send_socket:
                meshManager.getChatManagerHandler().sendEmptyMessage(MeshManagerHandler.CHAT_WRITE_MESSAGE_PING_A);
                return true;
            case R.id.action_connect_socket:
                meshManager.sendMessageToHandler(MeshManager.HANDLER_MESSAGE_FORCE_CLIENTSOCKET_TEST);
                return true;






            case R.id.action_wifi_disable_enable0:
                wifiManager.setWifiEnabled(false);
                try {
                    Thread.sleep(1500L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                wifiManager.setWifiEnabled(true);
                print_line("HACK", "Wifi disabled and enabled");
                return true;
            case R.id.action_ping_address:
                // ping public Internet sites without use of DNS
                meshManager.sendMessageToInternetTestHandler(InternetTestHandler.HANDLER_MESSAGE_INTERNET_ADDRESSONLY_TEST_A);
                return true;
            case R.id.action_wifi_p2p_rename0:


                return true;
            case R.id.action_wifi_p2p_force_on0:
                meshManager.sendMessageToHandler(MeshManager.HANDLER_MESSAGE_FORCE_WIFI_P2P_ON);
                return true;
            case R.id.action_website_test_reset0:
                internetTestIssuedCount = 0;
                internetTestIssuedFirstWhen = 0L;
                internetTestIssuedMostRecentWhen = 0L;
                return true;
            case R.id.action_wifi_ap_mode_try0:










                return true;
            case 999:
                // NetworkStatsManager
                /*
                IBinder nmBinder = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
                INetworkManagementService nmService = INetworkManagementService.Stub.asInterface(nmBinder);
                wifiManager.setIpForwardingEnabled(true);
                */
                break;
        }
        return super.onOptionsItemSelected(item);
    }
















    public boolean canWriteSettings()
    {
        boolean returnWriteSettings = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            returnWriteSettings = Settings.System.canWrite(this);
        }
        return returnWriteSettings;
    }
    public void callWriteSettings()
    {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
        startActivity(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        meshManager.sendMessageToHandler(MeshManager.HANDLER_MESSAGE_ACT_ON_ACTIVITY_DESTROY);

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

        SpannableString newContentSpannableString = TextStyleHelper.createSpannableString(line, Color.RED);

        logOutputSB.append(who + ": ");
        logOutputSB.append(newContentSpannableString);
        logOutputSB.append("\n");

        if (debugdataBox != null) {
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
                print_line("_AP", "inet address " + address.getHostAddress());

            }else if (WifiServiceSearcher.DSS_WIFISS_VALUES.equals(action)) {
                String s = intent.getStringExtra(WifiServiceSearcher.DSS_WIFISS_MESSAGE);
                routePrintMessage("_SS", s, isError);
            }else if (WifiServiceSearcher.DSS_WIFISS_PEERCOUNT.equals(action)) {
                int s = intent.getIntExtra(WifiServiceSearcher.DSS_WIFISS_COUNT, -1);
                print_line("_SS", "found " + s + " peers");
                mySpeech.speak(s + " peers discovered.");

            }else if (WifiServiceSearcher.DSS_WIFISS_PEERAPINFO.equals(action)) {
                String s = intent.getStringExtra(WifiServiceSearcher.DSS_WIFISS_INFOTEXT);

                String[] separated = s.split(":");
                print_line("_SS", "found SSID: " + separated[1] + ", pwd: "  + separated[2] + ", IP: " + separated[3]);
                meshManager.setWifiNetworkConfig(separated[1], separated[2], separated[3]);

                meshManager.sendMessageToHandler(MeshManager.HANDLER_MESSAGE_ACT_ON_WIFI_FOUND_SSID);
            }else if (WifiConnection.DSS_WIFICON_VALUES.equals(action)) {
                String s = intent.getStringExtra(WifiConnection.DSS_WIFICON_MESSAGE);
                print_line("CON", s);

            }else if (WifiConnection.DSS_WIFICON_SERVERADDRESS.equals(action)) {
                int addr = intent.getIntExtra(WifiConnection.DSS_WIFICON_INETADDRESS, -1);
                print_line("COM", "IP " + Formatter.formatIpAddress(addr));

                meshManager.sendMessageToHandler(MeshManager.HANDLER_MESSAGE_ACT_ON_WIFI_SERVERADDRESS, CLIENT_PORT_INSTANCE /* arg1 */);
            }else if (WifiConnection.DSS_WIFICON_STATUSVAL.equals(action)) {
                int status = intent.getIntExtra(WifiConnection.DSS_WIFICON_CONSTATUS, -1);

                String conStatus = "";
                if(status == WifiConnection.ConectionStateNONE) {
                    conStatus = "NONE";
                }else if(status == WifiConnection.ConectionStatePreConnecting) {
                    conStatus = "PreConnecting";
                }else if(status == WifiConnection.ConectionStateConnecting) {
                    conStatus = "Connecting";
                }else if(status == WifiConnection.ConectionStateConnected) {
                    conStatus = "Connected";
                    mySpeech.speak("Accesspoint connected");
                }else if(status == WifiConnection.ConectionStateDisconnected) {
                    conStatus = "Disconnected";
                    mySpeech.speak("Accesspoint Disconnected");

                    meshManager.sendMessageToHandler(MeshManager.HANDLER_MESSAGE_ACT_ON_WIFI_DISCONNECT);
                }

                print_line("COM", "Status " + conStatus + " [" + status + "]");
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
            else
            {
                print_line_error("???", "unrecognized action: " + action);
            }
        }
    }
}
