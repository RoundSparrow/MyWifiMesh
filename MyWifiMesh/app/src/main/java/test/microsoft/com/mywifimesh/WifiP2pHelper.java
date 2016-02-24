package test.microsoft.com.mywifimesh;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by adminsag on 2/23/16.
 * @author Stephen A. Gutknecht
 */
public class WifiP2pHelper {

	public static final String EXTRA_ERROR_INDICATOR = "ERROR";
	static final public String GENERAL_CLIENT_MESSAGE = "test.microsoft.com.mywifimesh.GENERAL_CLIENT_MESSAGE";
	public static final String GENERAL_MESSAGE_PAYLOAD = "MESSAGEPAYLOAD";
	private static LocalBroadcastManager globalBroadcastManager;


	public static String messageForErrorCode(int errorCode)
	{
		String reasonWords = "??";
		switch (errorCode)
		{
			case WifiP2pManager.BUSY:
				reasonWords = "BUSY";
				break;
			case WifiP2pManager.ERROR:
				reasonWords = "ERROR";
				break;
			case WifiP2pManager.P2P_UNSUPPORTED:
				reasonWords = "P2P_NO";
				break;
			case WifiP2pManager.NO_SERVICE_REQUESTS:
				reasonWords = "NO_SVC_REQS";
				break;
		}

		return reasonWords;
	}


	public static boolean forwardDebugPrint(LocalBroadcastManager broadcastManager, String broadcastTag, String messageTag, String buffer, boolean isError) {
		if (broadcastManager != null) {
			Intent intent = new Intent(broadcastTag);
			intent.putExtra(WifiP2pHelper.EXTRA_ERROR_INDICATOR, isError);
			if (buffer != null)
				intent.putExtra(messageTag, buffer);
			broadcastManager.sendBroadcast(intent);
			return true;
		}
		return false;
	}

	public static void setDebugLocalBroadcaster(LocalBroadcastManager broadcastManager)
	{
		globalBroadcastManager = broadcastManager;
	}

	public static void forwardDebugPrintGlobal(String messageTag, String messagePayload, boolean isError) {
		if (globalBroadcastManager == null)
		{
			Log.e("MyWifiMesh", "ERROR ERROR ERROR missing broadcastManager, message " + messageTag + ": " + messagePayload);
		}
		else
		{
			forwardDebugPrint(globalBroadcastManager, GENERAL_CLIENT_MESSAGE, GENERAL_MESSAGE_PAYLOAD, messagePayload, isError);
		}
	}
}
