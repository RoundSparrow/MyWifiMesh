package test.microsoft.com.mywifimesh;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by adminsag on 2/24/16.
 */
public class MeshManagerHandler extends Handler {
	public static String LOG_TAG = "MMHT";

	public static final int CHAT_MESSAGE_READ = 0x400 + 1;
	public static final int CHAT_WRITE_MESSAGE = 0x400 + 2;
	public static final int CHAT_WRITE_MESSAGE_PING_A = 0x400 + 3;

	int chatPingSendCount = 0;

	ChatManager chat = null;
	Handler chatMessageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case CHAT_MESSAGE_READ:

					byte[] readBuf = (byte[]) msg.obj;

					String  readMessage = new String(readBuf, 0, msg.arg1);

					print_line("_PM","Got message: " + readMessage);

					speakLine(readMessage);
					break;

				case CHAT_WRITE_MESSAGE:
					Object obj = msg.obj;
					chat = (ChatManager) obj;

					if (chat == null)
					{
						print_line_error("_PM", "chat null, can't send");
					}
					if (chat.getSocket() == null)
					{
						print_line_error("_PM", "chat Socket null, can't send");
					}

					String helloBuffer = "Hello There from " +  chat.getSide() + ": SDK v" + Build.VERSION.SDK_INT + " App v" + BuildConfig.VERSION_CODE;
					switch (chat.getSide())
					{
						case ChatManager.CHAT_SIDE_MANAGER:
							helloBuffer = "From group manager. SDK v" + Build.VERSION.SDK_INT + " App v" + BuildConfig.VERSION_CODE;
							break;
					}

					chat.write(helloBuffer.getBytes());
					print_line("_PM","Wrote message: " + helloBuffer);
					break;
				case CHAT_WRITE_MESSAGE_PING_A:
					if (chat == null)
					{
						print_line_error("_PM", "chat null, can't send");
					}
					if (chat.getSocket() == null)
					{
						print_line_error("_PM", "chat Socket null, can't send");
					}

					chatPingSendCount++;
					String sendBuffer = "ping " + chatPingSendCount;

					chat.write(sendBuffer.getBytes());
					print_line("_PM","Wrote message: " + sendBuffer);
					break;
				default:
					print_line_error("", "CODE_ERROR_WM0000_WHAT_MSG " + msg.what);
					break;
			}
		}
	};

	GroupOwnerSocketHandler  groupSocket = null;

	ClientSocketHandler clientSocket = null;

	WifiServiceSearcher    mWifiServiceSearcher = null;
	WifiAccessPoint        mWifiAccessPoint = null;
	WifiConnection         mWifiConnection = null;
	Boolean serviceRunning = false;
	Context appContext;

	public MeshManagerHandler(Looper looper, Context appContext) {
		super(looper);
		this.appContext = appContext;
	}

	public Handler getChatMessageHandler()
	{
		return chatMessageHandler;
	}

	@Override
	public void handleMessage(Message msg) {
		if (msg.what == 0) {
			Log.d(LOG_TAG, "got a message in " + Thread.currentThread() + ", now sleeping... ");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Log.d(LOG_TAG, "woke up, notifying ui thread...");
			// mUiHandler.sendEmptyMessage(1);
		} else
		if (msg.what == 1) {
			Log.d(LOG_TAG, "got a notification in " + Thread.currentThread());
		}
		else if (msg.what == MeshManager.HANDLER_MESSAGE_ACT_ON_WIFI_DISCONNECT)
		{
			// cleanup and recreate.
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
			mWifiAccessPoint = new WifiAccessPoint(appContext);
			mWifiAccessPoint.Start();

			if(mWifiServiceSearcher != null){
				mWifiServiceSearcher.Stop();
				mWifiServiceSearcher = null;
			}

			mWifiServiceSearcher = new WifiServiceSearcher(appContext);
			mWifiServiceSearcher.Start();
		}
		else if (msg.what == MeshManager.HANDLER_MESSAGE_ACT_ON_TOGGLE_CLICK)
		{
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
				print_line("_UI","Service Stopped");
			}else{
				serviceRunning = true;
				print_line("_UI","Service Started");

				mWifiAccessPoint = new WifiAccessPoint(appContext);
				mWifiAccessPoint.Start();

				mWifiServiceSearcher = new WifiServiceSearcher(appContext);
				mWifiServiceSearcher.Start();
			}
		}
		else if (msg.what == MeshManager.HANDLER_MESSAGE_ACT_ON_ACTIVITY_DESTROY)
		{
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
		}
		else if (msg.what == MeshManager.HANDLER_MESSAGE_ACT_ON_WIFI_FOUND_SSID)
		{
			if(mWifiConnection == null) {
				if(mWifiAccessPoint != null){
					mWifiAccessPoint.Stop();
					mWifiAccessPoint = null;
				}
				if(mWifiServiceSearcher != null){
					mWifiServiceSearcher.Stop();
					mWifiServiceSearcher = null;
				}

				WifiNetworkConfig networkConfig = MeshManager.getNetworkConfig();

				mWifiConnection = new WifiConnection(appContext, networkConfig.networkSSID, networkConfig.networkPassword);
				mWifiConnection.SetInetAddress(networkConfig.ipAddress);
				// MeshManager.setAccessPointFoundWhen(System.currentTimeMillis());
				speakLine("found Access point");
			}
		}
		else if (msg.what == MeshManager.HANDLER_MESSAGE_ACT_ON_WIFI_SERVERADDRESS)
		{
			if (clientSocket == null &&  mWifiConnection != null) {
				String IpToConnect = mWifiConnection.GetInetAddress();
				int port = msg.arg1;
				print_line("MMH", "Starting client socket connection to: " + IpToConnect + " port " + port);
				clientSocket = new ClientSocketHandler(chatMessageHandler, IpToConnect, port, appContext);
				clientSocket.start();
			}
			else
			{
				print_line_error("MMH", "No start. clientSocket null? " + (clientSocket == null) + " WifiCon null? " + (mWifiConnection == null));
			}
		}
		else if (msg.what == MeshManager.HANDLER_MESSAGE_FORCE_CLIENTSOCKET_TEST)
		{
			String targetIPAddress = "192.168.49.1";
			String myIPAddress = "nevermatch";
			if (MeshManager.getMeshState().netWifiPeerToPeerLikelyMyInet4Address0 != null)
			{
				myIPAddress = MeshManager.getMeshState().netWifiPeerToPeerLikelyMyInet4Address0.getHostAddress();
			}
			if (myIPAddress.equals(targetIPAddress))
			{
				print_line_error("MMH", "Forced start of client socket connection won't work, target InetAddress is my own! " + targetIPAddress);
			}
			else {
				print_line("MMH", "Starting client socket connection to: " + "192.168.49.1" + " port " + MainActivity.SOCKET_SERVER_PORT);
				clientSocket = new ClientSocketHandler(chatMessageHandler, "192.168.49.1", MainActivity.SOCKET_SERVER_PORT, appContext);
				clientSocket.start();
			}
		}
		else if (msg.what == MeshManager.HANDLER_MESSAGE_ACT_ON_GROUP_SOCKET_CREATE)
		{
			int serverPort = msg.arg1;
			try {
				if (groupSocket == null) {
					groupSocket = new GroupOwnerSocketHandler(chatMessageHandler, serverPort, appContext);
					groupSocket.start();
					print_line("_GS", "Group socketserver started on port " + serverPort + ".");
				}
				else
				{
					print_line("_GS", "Group socketserver already running?");
				}
			}
			catch (IOException e)
			{
				print_line_error("_GS", "IOException GroupOwnerSocketHandler");
			}
		}
		else if (msg.what == MeshManager.HANDLER_MESSAGE_FORCE_WIFI_P2P_ON)
		{
			try {
				Method turnOnWifiDirect = NsdManager.class.getDeclaredMethod("setEnabled", boolean.class);
				turnOnWifiDirect.setAccessible(true);
				turnOnWifiDirect.invoke(NsdManager.class, true);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				print_line_error("_??", "IllegalArgumentException " + e.getMessage());
				e.printStackTrace();;
			}
		}
		else
		{
			print_line_error("_??", "CODE_ERROR_WM1000_WHAT_MSG " + msg.what);
		}
	}


	public void print_line(String who,String line) {
		UserInterfaceOutputBox outputBox = new UserInterfaceOutputBox(who, line, UserInterfaceOutputBox.MESSAGE_TYPE_NORMAL);
		EventBus.getDefault().post(outputBox);
	}

	public void print_line_error(String who, String line) {
		UserInterfaceOutputBox outputBox = new UserInterfaceOutputBox(who, line, UserInterfaceOutputBox.MESSAGE_TYPE_ERROR);
		EventBus.getDefault().post(outputBox);
	}

	public void speakLine(String toSpeak)
	{
		UserInterfaceOutputBox outputBox = new UserInterfaceOutputBox("SPEAK", toSpeak, UserInterfaceOutputBox.MESSAGE_TYPE_SPEECH);
		EventBus.getDefault().post(outputBox);
	}
}