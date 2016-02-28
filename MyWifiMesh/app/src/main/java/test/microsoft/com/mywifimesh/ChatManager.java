
package test.microsoft.com.mywifimesh;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Handles reading and writing of messages with socket buffers. Uses a Handler
 * to post messages to UI thread for UI updates.
 */
public class ChatManager implements Runnable {

    public static final String CHAT_SIDE_UNKNOWN = "unknown";
    public static final String CHAT_SIDE_CLIENT = "client";
    public static final String CHAT_SIDE_MANAGER = "GroupChatManager";

    private Socket socket = null;
    private Handler chatManagerHandler;
    String side = CHAT_SIDE_UNKNOWN;

    public ChatManager(Socket socket, Handler handler, String who) {
        this.socket = socket;
        this.chatManagerHandler = handler;
        this.side = who;
    }

    private InputStream iStream;
    private OutputStream oStream;
    private static final String TAG = "ChatHandler";


    @Override
    public void run() {
        try {
            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();
            byte[] buffer = new byte[1048576]; //Megabyte buffer
            int bytesCount;

            chatManagerHandler.obtainMessage(MeshManagerHandler.CHAT_WRITE_MESSAGE, this).sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    bytesCount = iStream.read(buffer);
                    if (bytesCount == -1) {
                        break;
                    }

                    // Send the obtained bytes to the UI Activity
                    Log.d(TAG, "Rec:" + String.valueOf(buffer));
                    MeshManager.getMeshState().chatLastGoodIncomingWhen = System.currentTimeMillis();
                    chatManagerHandler.obtainMessage(MeshManagerHandler.CHAT_MESSAGE_READ, bytesCount, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    WifiP2pHelper.forwardDebugPrintGlobal("CM", "IOException Reading " + e.getMessage(), true /* ERROR */);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException ChatManager", e);
            WifiP2pHelper.forwardDebugPrintGlobal("CM", "IOException " + e.getMessage(), true /* ERROR */);
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // close on exception doesn't really need to be logged onscreen
                Log.e(TAG, "IOException ChatManager socket.close()");
            }
        }
        MeshManager.getMeshState().chatConnected = false;
    }

    public void write(byte[] buffer) {
        try {
            oStream.write(buffer);
            MeshManager.getMeshState().chatLastGoodWriteWhen = System.currentTimeMillis();
            MeshManager.getMeshState().chatConnected = true;
        } catch (IOException e) {
            MeshManager.getMeshState().chatLastFailedWriteWhen = System.currentTimeMillis();
            MeshManager.getMeshState().chatConnected = false;
            WifiP2pHelper.forwardDebugPrintGlobal("CM", "IOException on WRITE " + e.getMessage(), true /* ERROR */);
            Log.e(TAG, "Exception during write", e);
        }
    }

    String getSide(){
        return this.side;
    }

    public Socket getSocket() {
        return socket;
    }
}
