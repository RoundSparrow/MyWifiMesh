package test.microsoft.com.mywifimesh;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * Created by juksilve on 16.2.2015.
 */
public class MyP2PHelper {

    static public byte[] ipIntToBytes(int ip) {
        byte[] b = new byte[4];
        b[0] = (byte) (ip & 0xFF);
        b[1] = (byte) ((ip >> 8) & 0xFF);
        b[2] = (byte) ((ip >> 16) & 0xFF);
        b[3] = (byte) ((ip >> 24) & 0xFF);
        return b;
    }

    static public String ipAddressToString(InetAddress ip) {
        return ip.getHostAddress().replaceFirst("%.*", "");
    }

    static public String deviceToString(WifiP2pDevice device) {
        return device.deviceName + " " + device.deviceAddress;
    }

    static public boolean isValidIpAddress(String ip) {
        boolean v4 = InetAddressUtils.isIPv4Address(ip);
        boolean v6 = InetAddressUtils.isIPv6Address(ip);
        if(!v4 && !v6) return false;
        try {
            InetAddress inet = InetAddress.getByName(ip);
            return inet.isLinkLocalAddress() || inet.isSiteLocalAddress();
        } catch(UnknownHostException e) {
            //Log.e(TAG, e.toString());
            return false;
        }
    }

    static private NetworkInterface likelyTargetInterface0 = null;

    static public NetworkInterface getLikelyTargetInterface(Context context, boolean forceRebuild)
    {
        if (forceRebuild)
        {
            likelyTargetInterface0 = null;
        }
        if (likelyTargetInterface0 == null)
        {
            boolean goodBuild = buildInterfaceOutput(context);
        }
        return likelyTargetInterface0;
    }

    static private SpannableStringBuilder stuff;

    static private boolean buildInterfaceOutput(Context context)
    {
        List<NetworkInterface> ifaces;
        try {
            ifaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch(SocketException e) {
            stuff = new SpannableStringBuilder("Got error: " + e.toString());
            return false;
        }

        MeshManager.getMeshState().netWifiPeerToPeerLikelyMyInet4Address0 = null;
        MeshManager.getMeshState().netWifiPeerToPeerLikelyMyInet6Address0 = null;

        stuff = new SpannableStringBuilder("Local IP addresses: \n");
        for(NetworkInterface iface : ifaces) {
            for(InetAddress addr : Collections.list(iface.getInetAddresses())) {
                String interfaceAddress = MyP2PHelper.ipAddressToString(addr);

                SpannableString interfaceDescription = WifiSettingsHelper.getFormattedLinkTypeString(addr);

                SpannableString outInterfaceName = new SpannableString(iface.getName());
                // highlight the interfaces that look to be Wifi Direct
                if (iface.getName().startsWith("p2p"))
                {
                    outInterfaceName.setSpan(new ForegroundColorSpan(Color.BLUE), 0, outInterfaceName.length(), 0);
                    if (addr.isSiteLocalAddress()) {
                        likelyTargetInterface0 = iface;
                        if (addr instanceof Inet4Address)
                        {
                            MeshManager.getMeshState().netWifiPeerToPeerLikelyMyInet4Address0 = addr;
                        }
                        if (addr instanceof Inet6Address)
                        {
                            MeshManager.getMeshState().netWifiPeerToPeerLikelyMyInet6Address0 = addr;
                        }
                        interfaceDescription = TextStyleHelper.colorTextBoldText(interfaceDescription, Color.GREEN);
                    }
                }
                stuff.append("\t");
                stuff.append(outInterfaceName);
                stuff.append(": " );
                stuff.append(interfaceAddress);
                stuff.append(" (");
                stuff.append(interfaceDescription);
                stuff.append(")\n");
            }
        }

        return true;
    }


    static public void printLocalIpAddresses(Context context) {

        boolean goodBuild = buildInterfaceOutput(context);
        if (! goodBuild)
        {
            showDialogBox(stuff, context);
        }
        else {
            showDialogBox(stuff, context);
        }
    }

    // Dialog box
    public static void showDialogBox(SpannableStringBuilder message, Context context) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
