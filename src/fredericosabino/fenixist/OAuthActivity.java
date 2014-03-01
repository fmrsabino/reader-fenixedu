package fredericosabino.fenixist;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.gson.Gson;

public class OAuthActivity extends Activity {
	private String _url;
	private final String baseURL = "https://fenix.ist.utl.pt/oauth/userdialog?client_id=";
	private final String clientID = "7065221205721";
	private final String clientSecret = "KtgdcyFebV7rutKWH6p7TPR7aJAr385OygaYppsvPxuM636eweJs1ppYpHEQLXLTknqIw67jpCsAGGjLCR62bgwpEv2GGtyHa3t0g73xdyeqmmwHIl3";
	private final String urlRedirect = "http://localhost:8080/authorization";
	private String code;
	private TokenValidator tokenValidatorTask; //needed for the cancellation of the task
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_screen);
		
		_url = baseURL + clientID + "&redirect_uri=" + urlRedirect;
		
		SharedPreferences settings = getSharedPreferences("OAuthToken", MODE_PRIVATE);
		String accessToken = settings.getString("accessToken", null);
		if(accessToken == null) {
			//We need to get a new access token + refresh token
			Log.v("OAUTH", "Token doesn't exist. Getting new one...");
			
			//Check Internet Connection
			ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if(networkInfo != null && networkInfo.isConnected()) {
				new DownloadPage().execute(_url); //[login]+NewToken
			} else {
				setContentView(R.layout.no_connection);
			}
		}
		else {
			View view = findViewById(R.id.signingLoading);
			view.setVisibility(View.VISIBLE);
			Log.v("OAUTH", "Token exists and its value is: " + accessToken);
			
			//Check Internet Connection
			ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if(networkInfo != null && networkInfo.isConnected()) {
				//See if the token is still valid with a simple GET request
				tokenValidatorTask = (TokenValidator) new TokenValidator().execute(accessToken);
			} else {
				enterApp(); //enter app even without checking for the token but it must be check with later requests
			}
		}
	}
	
	public void retryConnection(View v) {
		Log.i("OAuthActivity", "Starting retryConnection");
		
		//Check Internet Connection
		ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if(networkInfo != null && networkInfo.isConnected()) {
			View text = findViewById(R.id.noConnection);
			text.setVisibility(View.GONE);
			View button = findViewById(R.id.retryConnection);
			button.setVisibility(View.GONE);
			View progress = findViewById(R.id.progressBar1);
			progress.setVisibility(View.VISIBLE);
			new DownloadPage().execute(_url); //[login]+NewToken
		} else {
			//make toast
		}
	}
	
	/*This is called after all the verifications with the access to the account info are done*/
	public void enterApp() {
		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); 
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
	/**
	 * Receives an url and downloads the contents from the webserver specified by this url
	 */
	public String downloadContents(String url) throws IOException, MalformedURLException {		
		AndroidHttpClient httpclient = AndroidHttpClient.newInstance("", this);
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		InputStream stream = entity.getContent();
		httpclient.close();
		return convertStreamToString(stream);
	}
	
	static String convertStreamToString(java.io.InputStream is) {
	    @SuppressWarnings("resource")
		java.util.Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	
	public void getToken() throws IOException {
		if(code == null) {
			Log.e("OAUTH", "Code is null!!");
		}
		new TokenTask().execute();
	}
	
	private String downloadToken() {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("https://fenix.tecnico.ulisboa.pt/oauth/access_token");
	    String result = "";
	    UrlEncodedFormEntity test;
	    httppost.setHeader("Content-Type",
                "application/x-www-form-urlencoded");
	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
	        
	        nameValuePairs.add(new BasicNameValuePair("client_id", clientID));
	        nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));
	        nameValuePairs.add(new BasicNameValuePair("redirect_uri", urlRedirect));
	        nameValuePairs.add(new BasicNameValuePair("code", code));
	        nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
	        
	        
	        test = new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8);
	        Log.v("OAUTH", convertStreamToString(test.getContent()));
	        httppost.setEntity(test);

	        // Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httppost);
	        
	        
	        Log.v("OAUTH", "HTTP Status: " + response.getStatusLine().getStatusCode());
	        result = EntityUtils.toString(response.getEntity());
	        
	        
	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	    }
	    
	    Log.v("OAUTH", "Post Result is: " + result);
	    return result;
	}
	
	
	private class TokenTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			return downloadToken();
		}
		
		/*Here we have the access token and refresh token*/
		@Override
		protected void onPostExecute(String result) {
			Gson gson = new Gson();
			TokenObject token = gson.fromJson(result, TokenObject.class);
				
			Log.v("OAUTH", "JSON parsing finished");
			Log.v("OAUTH", "Expiration time: " + token.getExpires_in());
			Log.v("OAUTH", "Refresh Token: " + token.getRefresh_token());
			Log.v("OAUTH", "Access Token: " + token.getAccess_token());
			
			/*Save the token information in OAuthToken*/
			SharedPreferences settings = getSharedPreferences("OAuthToken", MODE_PRIVATE);
			SharedPreferences.Editor editor  = settings.edit();
			editor.putInt("expirationTime", token.getExpires_in());
			editor.putString("refreshToken", token.getRefresh_token());
			editor.putString("accessToken", token.getAccess_token());
			editor.commit();
			
			enterApp();
		}
	}
	
	private class TokenObject {
		private int expires_in; //only when creating the token
		private String refresh_token;
		private String access_token;
		private int expires; //only when refreshing the token
		
		public int getExpires() {
			return expires;
		}
		
		public int getExpires_in() {
			return expires_in;
		}
		public String getRefresh_token() {
			return refresh_token;
		}
		public String getAccess_token() {
			return access_token;
		}
	}
	
	private class DownloadPage extends AsyncTask<String, Void, String> {
		
		@Override
		protected void onPreExecute() {
			setContentView(R.layout.activity_oauth);
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeAllCookie();
			/*View view = findViewById(R.id.oauth_signUp_wait);
			view.setVisibility(View.GONE);*/
			View download = findViewById(R.id.oauth_page_download);
			download.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			try {
				return downloadContents(params[0]);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return _url;
		}
		
		@SuppressLint("SetJavaScriptEnabled")
		@Override
		protected void onPostExecute(String result) {
			View view = findViewById(R.id.oauth_page_download);
			view.setVisibility(View.GONE);
			
			WebView web = (WebView) findViewById(R.id.oauth_page);
			WebSettings websettings = web.getSettings();
			websettings.setJavaScriptEnabled(true);
			web.clearCache(true);
			web.setWebViewClient(new HelloWebViewClient());
			web.loadUrl(_url);
			if(code != null) {
				try {
					getToken();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/*For URL redirects*/
	private class HelloWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    URL url2 = null;
		try {
			url2 = new URL(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    //view.loadUrl(url);
	    if(url2.getHost().endsWith("ist.utl.pt") || url2.getHost().endsWith("tecnico.ulisboa.pt")) {
	            return false;
	    }
	    if(url2.getHost().endsWith("localhost")) {
	    	Log.v("OAUTH", "Code Page!");
	    	Log.v("OAUTH", "Code is: " + url2.getQuery());
	    	
	    	/*Parse of the code*/
	    	code = (Uri.parse(url)).getQueryParameter("code");
	    	
	    	Log.v("OAUTH", code);
	    	try {
				getToken();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    	
	    //Log.v("OAUTH", "Not a fenix link!");
	    return true;
	    }
	}

	private class TokenValidator extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... arg0) {
			return validateToken(arg0[0]);
		}
		
		/*Parse the result - save the new access token*/
		@Override
		protected void onPostExecute(String result) {
			if(result == null) {
				Log.v("OAUTH Refresh", "No need to refresh the token");
			}
			else {
				Log.v("OAUTH Refresh", "Token needs to be refreshed");
				Gson gson = new Gson();
				TokenObject token = gson.fromJson(result, TokenObject.class);
					
				Log.v("OAUTH Refresh", "Expiration time: " + token.getExpires());
				Log.v("OAUTH Refresh", "Access Token: " + token.getAccess_token());
				
				/*Save the token information in OAuthToken*/
				SharedPreferences settings = getSharedPreferences("OAuthToken", MODE_PRIVATE);
				SharedPreferences.Editor editor  = settings.edit();
				editor.putInt("expirationTime", token.getExpires());
				editor.putString("accessToken", token.getAccess_token());
				editor.commit();
			}
			
			//ready to move to the app!
			enterApp();
		}
		
		@Override
		protected void onCancelled() {
			new DownloadPage().execute(_url); //The user revoked the application
		}
		
	}
	
	private String validateToken(String token) {
		HttpClient httpclient = new DefaultHttpClient();
	    HttpGet httpget = new HttpGet("https://fenix.tecnico.ulisboa.pt/api/fenix/v1/person?" + "access_token=" + token);
	    String result = null;
	    try {
			HttpResponse response = httpclient.execute(httpget);
			Log.v("OAUTH VALIDATE", "HTTP Status: " + response.getStatusLine().getStatusCode());
			if(response.getStatusLine().getStatusCode() == 401) { //Unauthorized
				result = refreshToken(); //we need to refresh the token
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return result;
	}
	
	/*If the refresh token expires it is because the user revoked the application and to get a new one it should redirect to the login page*/
	private String refreshToken() {
		SharedPreferences settings = getSharedPreferences("OAuthToken", MODE_PRIVATE);
		String refreshToken = settings.getString("refreshToken", null); //should never be null at this stage
		
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("https://fenix.tecnico.ulisboa.pt/oauth/refresh_token");
	    String result = "";
	    UrlEncodedFormEntity request = null;
	    httppost.setHeader("Content-Type",
                "application/x-www-form-urlencoded");
	    // Add your data
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
	        
	    nameValuePairs.add(new BasicNameValuePair("client_id", clientID));
	    nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));
	    nameValuePairs.add(new BasicNameValuePair("refresh_token", refreshToken));
	    nameValuePairs.add(new BasicNameValuePair("grant_type", "refresh_token"));
	    
	    try {
			request = new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8);
			Log.v("OAUTH", "Refresh Request: " + convertStreamToString(request.getContent()));
	        httppost.setEntity(request);
	        
	        // Execute HTTP Post Request
	        HttpResponse response;
			response = httpclient.execute(httppost);
			
			Log.v("OAUTH", "HTTP Status: " + response.getStatusLine().getStatusCode());
	        if(response.getStatusLine().getStatusCode() != 200) { //Something is not right -> App Revoke Situation
				//Request the user permission for the app -> [Login]+Request
	        	//cancels the task so it doesn't continue through the process of refreshing the token
	        	Log.v("OAUTH", "The user revoked the application.");
	        	tokenValidatorTask.cancel(true);
	        	return null;
			}
	        result = EntityUtils.toString(response.getEntity());
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

}
