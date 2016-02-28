package test.microsoft.com.mywifimesh;

/**
 * Created by adminsag on 2/24/16.
 */
public class UserInterfaceOutputBox {
	public static final int MESSAGE_TYPE_NORMAL = 0;
	public static final int MESSAGE_TYPE_ERROR  = 1;
	public static final int MESSAGE_TYPE_SPEECH = 10;

	public String whoFrom = "";
	public String outputMessage = "";
	// messageType 0 = normal, 1 = error
	public int    messageType = 0;

	public UserInterfaceOutputBox(String who, String message, int messageType) {
		this.messageType = messageType;
		this.whoFrom = who;
		this.outputMessage = message;
	}
}
