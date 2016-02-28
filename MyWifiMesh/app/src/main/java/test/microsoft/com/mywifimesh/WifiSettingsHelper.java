package test.microsoft.com.mywifimesh;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.SpannableString;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by adminsag on 2/27/16.
 */
public class WifiSettingsHelper {

	public static WifiConfiguration getCurrentWifiConfiguration(Context context)
	{
		WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		WifiConfiguration wifiConf = null;
		if ( manager.getConfiguredNetworks() != null) {
			for (WifiConfiguration conn : manager.getConfiguredNetworks()) {
				if (conn.status == WifiConfiguration.Status.CURRENT) {
					wifiConf = conn;
					break;
				}
			}
		}
		return wifiConf;
	}

	public static WifiInfo getCurrentWifiInfo(Context context) {
		WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		return manager.getConnectionInfo();
	}

	public static int convertFrequencyToChannel(int freq) {
		if (freq >= 2412 && freq <= 2484) {
			return (freq - 2412) / 5 + 1;
		} else if (freq >= 5170 && freq <= 5825) {
			return (freq - 5170) / 5 + 34;
		} else {
			return -1;
		}
	}


	/*
	Android 6.0 change:
	getMacAddress will return fake "02:00:00:00:00:00"
	http://stackoverflow.com/questions/33159224/getting-mac-address-in-android-6-0
	 */
	public static Enumeration<InetAddress> getWifiInetAddresses(final Context context) {
		final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		final WifiInfo wifiInfo = wifiManager.getConnectionInfo();

		byte[] macBytes = getWifiMacAddressBytesSmart(wifiInfo);
		try {
			final Enumeration<NetworkInterface> e =  NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				final NetworkInterface networkInterface = e.nextElement();
				if (Arrays.equals(networkInterface.getHardwareAddress(), macBytes)) {
					return networkInterface.getInetAddresses();
				}
			}
		} catch (SocketException e) {
			// ALog.e("Unable to NetworkInterface.getNetworkInterfaces()");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static<T extends InetAddress> T getWifiInetAddress(final Context context, final Class<T> inetClass) {
		final Enumeration<InetAddress> e = getWifiInetAddresses(context);
		while (e.hasMoreElements()) {
			final InetAddress inetAddress = e.nextElement();
			if (inetAddress.getClass() == inetClass) {
				return (T)inetAddress;
			}
		}
		return null;
	}

	public static void exampleUsageA(Context context)
	{
		final Inet4Address inet4Address = getWifiInetAddress(context, Inet4Address.class);
		final Inet6Address inet6Address = getWifiInetAddress(context, Inet6Address.class);
	}


	public static byte[] getWifiMacAddressBytesSmart(WifiInfo wifiInfo)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
			try {
				List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
				for (NetworkInterface intf : interfaces) {
					if (!intf.getName().equalsIgnoreCase("wlan0")) {
						continue;
					}

					byte[] mac = intf.getHardwareAddress();
					return mac;
				}
			}
			catch (IOException e0)
			{
				return new byte[0];
			}
		}
		else {
			final String macAddress = wifiInfo.getMacAddress();
			final String[] macParts = macAddress.split(":");
			final byte[] macBytes = new byte[macParts.length];
			for (int i = 0; i < macParts.length; i++) {
				macBytes[i] = (byte) Integer.parseInt(macParts[i], 16);
			}
			return macBytes;
		}
		return new byte[0];
	}

	// http://stackoverflow.com/questions/31329733/how-to-get-the-missing-wifi-mac-address-on-android-m-preview
	public static String getAndroid6MacAddress()
	{
		try {
			String interfaceName = "wlan0";
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				if (!intf.getName().equalsIgnoreCase(interfaceName)){
					continue;
				}

				byte[] mac = intf.getHardwareAddress();
				if (mac==null){
					return "";
				}

				StringBuilder buf = new StringBuilder();
				for (byte aMac : mac) {
					buf.append(String.format("%02X:", aMac));
				}
				if (buf.length()>0) {
					buf.deleteCharAt(buf.length() - 1);
				}
				return buf.toString();
			}
		} catch (Exception ex) { } // for now eat exceptions
		return "";
	}

	public static SpannableString getFormattedLinkTypeString(InetAddress addr)
	{
		SpannableString interfaceDescription = new SpannableString("unmatched");
		// Do any-local first, it may be replace by better.
		if(addr.isAnyLocalAddress())  interfaceDescription = new SpannableString("any-local");
		if(addr.isLoopbackAddress())  interfaceDescription = new SpannableString("loopback");
		if(addr.isLinkLocalAddress()) interfaceDescription = new SpannableString("link-local");
		if(addr.isSiteLocalAddress()) interfaceDescription = new SpannableString("site-local");
		if(addr.isMulticastAddress()) interfaceDescription = new SpannableString("multicast");

		if (interfaceDescription.equals(new SpannableString("unmatched")))
		{
			if (addr instanceof Inet6Address)
			{
				// ToDo: also experiment with ((Inet6Address) addr).getScopeId()
				if (addr.getHostAddress().endsWith("%5"))
				{
					interfaceDescription = new SpannableString("site-local_%5");
				}
			}
		}
		return interfaceDescription;
	}
}
