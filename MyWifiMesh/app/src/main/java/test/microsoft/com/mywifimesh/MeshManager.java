package test.microsoft.com.mywifimesh;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

/**
 * Created by adminsag on 2/24/16.
 */
public class MeshManager {

	public static final int HANDLER_MESSAGE_ACT_ON_WIFI_DISCONNECT = 100;
	public static final int HANDLER_MESSAGE_ACT_ON_TOGGLE_CLICK = 110;
	public static final int HANDLER_MESSAGE_ACT_ON_ACTIVITY_DESTROY = 300;
	public static final int HANDLER_MESSAGE_ACT_ON_WIFI_FOUND_SSID = 120;
	public static final int HANDLER_MESSAGE_ACT_ON_WIFI_SERVERADDRESS = 130;
	public static final int HANDLER_MESSAGE_ACT_ON_GROUP_SOCKET_CREATE = 140;
	public static final int HANDLER_MESSAGE_FORCE_CLIENTSOCKET_TEST = 150;
	public static final int HANDLER_MESSAGE_FORCE_WIFI_P2P_ON = 170;

	private volatile Looper meshManagerLooper;
	private volatile MeshManagerHandler meshManagerHandler;
	private          HandlerThread meshManagerThread;
	private volatile InternetTestHandler internetTestHandler;
	private          HandlerThread internetTestThread;
	private volatile Looper internetTestLooper;
	private 		 Handler chatManagerHandler = null;

	private static WifiNetworkConfig networkConfig = new WifiNetworkConfig();
	private static MeshState meshState = new MeshState();


	public void setupMeshManager(Context appContext)
	{
		meshManagerThread = new HandlerThread("MeshManagerThread");
		meshManagerThread.start();

		meshManagerLooper = meshManagerThread.getLooper();
		meshManagerHandler = new MeshManagerHandler(meshManagerLooper, appContext);

		internetTestThread = new HandlerThread("InternetTestThread");
		internetTestThread.start();
		internetTestLooper = internetTestThread.getLooper();
		internetTestHandler = new InternetTestHandler(internetTestLooper, appContext);

		chatManagerHandler = meshManagerHandler.getChatMessageHandler();
	}

	public Handler getChatManagerHandler()
	{
		return chatManagerHandler;
	}

	public void sendMessageToHandler(int messageWhat)
	{
		Message msg = meshManagerHandler.obtainMessage();
		msg.what = messageWhat;
		// msg.obj = <Object>;
		meshManagerHandler.sendMessage(msg);
	}

	public void sendMessageToHandler(int messageWhat, int messageArg1)
	{
		Message msg = meshManagerHandler.obtainMessage();
		msg.what = messageWhat;
		msg.arg1 = messageArg1;
		// msg.obj = <Object>;
		meshManagerHandler.sendMessage(msg);
	}

	public static MeshState getMeshState()
	{
		return meshState;
	}

	public void setWifiNetworkConfig(String networkSSID, String networkPassword, String networkIpAddress)
	{
		networkConfig.ipAddress = networkIpAddress;
		networkConfig.networkSSID = networkSSID;
		networkConfig.networkPassword = networkPassword;
	}

	public static WifiNetworkConfig getNetworkConfig()
	{
		return networkConfig;
	}


	public static void setAccessPointFoundWhen(long inWhen) {
		meshState.accessPointFound = true;
		meshState.accessPointFoundWhen = inWhen;
	}

	public static long getAccessPointFoundWhen()
	{
		return meshState.accessPointFoundWhen;
	}

	public void sendMessageToInternetTestHandler(int messageWhat) {
		Message msg = internetTestHandler.obtainMessage();
		msg.what = messageWhat;
		// msg.obj = <Object>;
		internetTestHandler.sendMessage(msg);
	}
}
