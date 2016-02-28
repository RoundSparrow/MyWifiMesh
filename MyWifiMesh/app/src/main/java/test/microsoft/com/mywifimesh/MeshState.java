package test.microsoft.com.mywifimesh;

import android.net.wifi.WifiInfo;

import java.net.InetAddress;

/**
 * Created by adminsag on 2/25/16.
 */
public class MeshState {
    public long accessPointFoundWhen = 0L;
    public boolean accessPointFound = false;

    public boolean chatConnected = false;
    public long chatLastGoodWriteWhen = 0L;
    public long chatLastFailedWriteWhen = 0L;
    public long chatLastGoodIncomingWhen = 0L;
    public long peersFoundCount = 0;

    public int peer2PeerOnChannelDisconnectCount = 0;
    public long peer2PeerDiscoverPeersSuccessWhen = 0L;
    public int netWifiConnectedCount = 0;
    public int peer2PeerGroupClientCount = 0;

    public int chatServerSocketAcceptCount = 0;

    public WifiInfo netWifiGotInfo = null;

    public String peerDiscoveryLogA = "";

    // What I believe to be the simple version of my local P2P WiFi interface
    public InetAddress netWifiPeerToPeerLikelyMyInet4Address0 = null;
    public InetAddress netWifiPeerToPeerLikelyMyInet6Address0 = null;

    public boolean internetConnectionTestWebsiteAGood = false;
    public long internetConnectionTestWebsiteAGoodWhen = 0L;
    public String internetConnectionTestWebsiteAcontentMostRecent = "";
}
