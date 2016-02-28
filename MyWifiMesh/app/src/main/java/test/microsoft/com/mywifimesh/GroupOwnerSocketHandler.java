
package test.microsoft.com.mywifimesh;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The implementation of a ServerSocket chatManagerHandler. This is used by the wifi p2p
 * group owner.
 */
public class GroupOwnerSocketHandler extends Thread {

    static final public String DSS_GROUP_VALUES = "test.microsoft.com.mywifimesh.DSS_GROUP_VALUES";
    static final public String DSS_GROUP_MESSAGE = "test.microsoft.com.mywifimesh.DSS_GROUP_MESSAGE";

    LocalBroadcastManager broadcaster;
    ServerSocket socket = null;
    private Handler chatManagerHandler;
    private static final String TAG = "GroupOwnerSocketHandler";
    private ChatManager chat;


    public GroupOwnerSocketHandler(Handler handler, int port,Context context) throws IOException {
        try {
            this.broadcaster = LocalBroadcastManager.getInstance(context);
            socket = new ServerSocket(port);
            this.chatManagerHandler = handler;
            Log.d("GroupOwnerSocketHandler", "Socket Started on port " + port);
        } catch (Exception e) {
            WifiP2pHelper.forwardDebugPrint(broadcaster, DSS_GROUP_VALUES, DSS_GROUP_MESSAGE, e.toString(), true /* ERROR */);
            throw e;
        }
    }

    /**
     * A ThreadPool for client sockets.

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

     */
    @Override
    public void run() {
        while (true) {
            try {
                // A blocking operation. Initiate a ChatManager instance when
                // there is a new connection
                Socket  s = socket.accept();
                MeshManager.getMeshState().chatServerSocketAcceptCount++;
                Log.d(TAG, "Launching the Group I/O handler (GroupOwnerSocketHandler)");
                // ToDo: one single ChatManager for all clients? Or should this be a local variable to allow multiple incoming clients?
                chat = new ChatManager(s, chatManagerHandler, ChatManager.CHAT_SIDE_MANAGER);
                new Thread(chat).start();

            } catch (Exception e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (Exception ioe) {
                    WifiP2pHelper.forwardDebugPrint(broadcaster, DSS_GROUP_VALUES, DSS_GROUP_MESSAGE, ioe.toString(), true /* ERROR */);
                }

                WifiP2pHelper.forwardDebugPrint(broadcaster, DSS_GROUP_VALUES, DSS_GROUP_MESSAGE, e.toString(), true /* ERROR */);
                break;
            }
        }
    }


    public ChatManager getChat() {
        return chat;
    }
}
