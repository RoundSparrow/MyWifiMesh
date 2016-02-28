
package test.microsoft.com.mywifimesh;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketHandler extends Thread {

    static final public String DSS_CLIENT_VALUES = "test.microsoft.com.mywifimesh.DSS_CLIENT_VALUES";
    static final public String DSS_CLIENT_MESSAGE = "test.microsoft.com.mywifimesh.DSS_CLIENT_MESSAGE";
    static final public int SOCKET_CONNECT_TIMEOUT = 6000;

    LocalBroadcastManager broadcaster;
    private static final String TAG = "ClientSocketHandler";
    private Handler chatManagerHandler;
    private ChatManager chat;
    private String mAddress;
    private int mPort;


    public ClientSocketHandler(Handler handler, String groupOwnerAddress, int port,Context context) {
        this.broadcaster = LocalBroadcastManager.getInstance(context);
        this.chatManagerHandler = handler;
        this.mAddress = groupOwnerAddress;
        this.mPort = port;
    }


    @Override
    public void run() {
        InetSocketAddress serverSocketAddress = new InetSocketAddress(mAddress,mPort);
        WifiP2pHelper.forwardDebugPrint(broadcaster, DSS_CLIENT_VALUES, DSS_CLIENT_MESSAGE, "Attempting socket to server... [" + serverSocketAddress.toString() + "]", false /* Not error */);
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(serverSocketAddress, SOCKET_CONNECT_TIMEOUT);
            Log.d(TAG, "Launching the I/O handler (ChatManager)");
            chat = new ChatManager(socket, chatManagerHandler, ChatManager.CHAT_SIDE_CLIENT);
            new Thread(chat).start();

        } catch (Exception e) {
            Log.e(TAG, "Exception ChatManager");
            e.printStackTrace();
            WifiP2pHelper.forwardDebugPrint(broadcaster, DSS_CLIENT_VALUES, DSS_CLIENT_MESSAGE, e.toString(), true /* ERROR */);
            try {
                socket.close();
            } catch (Exception e1) {
                WifiP2pHelper.forwardDebugPrint(broadcaster, DSS_CLIENT_VALUES, DSS_CLIENT_MESSAGE, e.toString(), true /* ERROR */);
            }
        }
    }



    public ChatManager getChat() {
        return chat;
    }

}
