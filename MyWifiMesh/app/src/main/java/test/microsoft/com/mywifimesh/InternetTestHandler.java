package test.microsoft.com.mywifimesh;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by adminsag on 2/26/16.
 */
public class InternetTestHandler extends Handler {

	public static String LOG_TAG = "PIT";
	public static final int HANDLER_MESSAGE_INTERNET_WEBSITE_TEST_A = 160;
	public static final int HANDLER_MESSAGE_INTERNET_ADDRESSONLY_TEST_A = 161;

	Context appContext;


	public InternetTestHandler(Looper looper, Context appContext) {
		super(looper);
		this.appContext = appContext;
	}

	@Override
	public void handleMessage(Message msg) {
		if (msg.what == 0) {

		}
		else if (msg.what == InternetTestHandler.HANDLER_MESSAGE_INTERNET_WEBSITE_TEST_A)
		{
			// Note: this will delay the next tick of the timer while we do this test
			MeshState meshState = MeshManager.getMeshState();
			String targetWebpageOrEmptyString = InternetHelper.fetchInternetWebsiteA();
			if (targetWebpageOrEmptyString.length() > 0)
			{
				meshState.internetConnectionTestWebsiteAGood = true;
				meshState.internetConnectionTestWebsiteAGoodWhen = System.currentTimeMillis();
				meshState.internetConnectionTestWebsiteAcontentMostRecent = targetWebpageOrEmptyString;
				print_line("MMH", "InternetTest WebsiteA good: " + targetWebpageOrEmptyString);
			}
			else
			{
				meshState.internetConnectionTestWebsiteAGood = false;
				print_line_error("MMH", "InternetTest WebsiteA FAILED");
			}
		}
		else if (msg.what == InternetTestHandler.HANDLER_MESSAGE_INTERNET_ADDRESSONLY_TEST_A)
		{
			try {
				int failedSoFar = 0;
				Log.i(LOG_TAG, "ping host 8.8.8.8");
				InetAddress testHostA = InetAddress.getByName("8.8.8.8");
				if (testHostA.isReachable(5000))
				{
					print_line("MMH", "ping " + testHostA.getHostAddress() + " worked");
				}
				else
				{
					print_line_error("MMH", "ping " + testHostA.getHostAddress() + " unreachable?");
					failedSoFar++;
				}
				// www.cisco.com 23.41.3.24
				Log.i(LOG_TAG, "ping host 23.41.3.24");
				InetAddress testHostB = InetAddress.getByName("23.41.3.24");
				if (testHostB.isReachable(5000))
				{
					print_line("MMH", "ping " + testHostB.getHostAddress() + " worked");
				}
				else
				{
					print_line_error("MMH", "ping " + testHostB.getHostAddress() + " unreachable?");
					failedSoFar++;
				}

				if (failedSoFar >= 2)
				{
					// both failed?
					// http://ip-api.com/json with IP Address instead of domain name
					String targetWebpageOrEmptyString = InternetHelper.fetchInternetWebsiteUrlString("http://162.250.144.215/json");
					if (targetWebpageOrEmptyString.length() > 0)
					{
						print_line("MMH", "webfetch ping worked, length " + targetWebpageOrEmptyString.length());
					}
					else
					{
						print_line_error("MMH", "webfetch ping failed too");
					}
				}
			} catch (UnknownHostException e) {
				print_line_error("MMH", "ping failed, UnknownHostException");
				e.printStackTrace();
			} catch (IOException e0) {
				print_line_error("MMH", "ping failed, IOException");
				e0.printStackTrace();
			}
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
