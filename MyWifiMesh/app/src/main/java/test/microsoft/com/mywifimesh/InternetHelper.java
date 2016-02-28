package test.microsoft.com.mywifimesh;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by adminsag on 2/25/16.
 */
public class InternetHelper {

	/*
	Note on Android SDK 23:
	   "This preview removes support for the Apache HTTP client. If your app is using this client and targets Android 2.3 (API level 9) or higher, use the HttpURLConnection class instead. This API is more efficient because it reduces network use through transparent compression and response caching, and minimizes power consumption"
	"Prior to Android 2.2 (Froyo), this class had some frustrating bug"
	*/
	public static boolean testInternetWebsiteUrlString(String targetWebsiteUrlString)
	{
		URL url;
		try {
			url = new URL(targetWebsiteUrlString);
		}
		catch (Exception e0)
		{
			return false;
		}

		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection) url.openConnection();
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			String pageRead = readStream(in);
			if (pageRead.contains("."))
			{
				return true;
			}
			return false;
		}
		catch (IOException e0)
		{
			return false;
		}
		finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}

	public static String fetchInternetWebsiteUrlString(String inUrlString)
	{
		URL url;
		try {
			url = new URL(inUrlString);
		}
		catch (Exception e0)
		{
			return "";
		}

		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection) url.openConnection();
			// don't follow redirects, more pure test
			urlConnection.setInstanceFollowRedirects(false);
			urlConnection.setConnectTimeout(1000*4);
			urlConnection.setReadTimeout(2500);
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			String pageRead = readStream(in);
			if (pageRead.contains("."))
			{
				return pageRead;
			}
			return "";
		}
		catch (IOException e0)
		{
			return "";
		}
		finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}


	private static String readStream(InputStream in) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String nextLine = "";
			while ((nextLine = reader.readLine()) != null) {
				sb.append(nextLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static String fetchInternetWebsiteA() {
		return fetchInternetWebsiteUrlString("https://api.ipify.org?format=json");
	}
}
