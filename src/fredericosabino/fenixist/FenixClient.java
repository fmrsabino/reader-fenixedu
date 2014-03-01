package fredericosabino.fenixist;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

import com.google.gson.Gson;

import fredericosabino.fenixist.exceptions.NoConnectionException;
import fredericosabino.fenixist.fenixdata.CourseInfo;
import fredericosabino.fenixist.fenixdata.UserCoursesInfo;

public class FenixClient {
	private String TAG = "FENIX CLIENT"; //debug
	private String accessToken;
	Activity _activity; //reference to store data with SharedPreferences
	
	FenixClient(Activity activity) {
		_activity = activity;
		SharedPreferences settings = activity.getSharedPreferences("OAuthToken", Activity.MODE_PRIVATE);
		accessToken = settings.getString("accessToken", null);
	}
	
	public UserCoursesInfo getUserCourses() throws NoConnectionException {
		String result = null;
		String baseURL = "https://fenix.tecnico.ulisboa.pt/api/fenix/v1/person/courses?access_token=" + accessToken;
		FenixDownloader task = (FenixDownloader) new FenixDownloader().execute(baseURL);
		
		try {
			result = task.get(); //blocks UI Thread :( TODO: Download indicator + exception treatment
			if(result == null) {
				throw new NoConnectionException();
			}
			return parseCourses(result);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null; //no courses
	}
	
	public UserCoursesInfo parseCourses(String result) throws IOException {
		Gson gson = new Gson();
		UserCoursesInfo courses = gson.fromJson(result, UserCoursesInfo.class);
		return courses;
	}
	
	public CourseInfo getCourseInfo(long courseID) {
		String result = null;
		String baseURL = "https://fenix.tecnico.ulisboa.pt/api/fenix/v1/courses/" + courseID;
		FenixDownloader task = (FenixDownloader) new FenixDownloader().execute(baseURL);
		
		try {
			result = task.get(); //blocks UI Thread :( TODO: Download indicator + exception treatment
			return parseCourseInfo(result);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null; //error
	}
	
	public CourseInfo parseCourseInfo(String result) {
		Gson gson = new Gson();
		CourseInfo course = gson.fromJson(result, CourseInfo.class);
		return course;
	}
	
	/*Downloads a GET URL command*/
	private class FenixDownloader extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			try {
				return downloadFromFenix(params[0]);
			} catch (NoConnectionException e) {
				return null;
			}
		}
	}
	
	private String downloadFromFenix(String url) throws NoConnectionException {
		AndroidHttpClient httpclient = AndroidHttpClient.newInstance("", _activity);
	    HttpGet httpget = new HttpGet(url);
	    String result = null;
	    
	    //Download
		try {
			//Check internet connection
			ConnectivityManager connMgr = (ConnectivityManager) _activity.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if(networkInfo != null && networkInfo.isConnected()) {
				HttpResponse response = httpclient.execute(httpget);
				result = EntityUtils.toString(response.getEntity());
				httpclient.close();
			} else {
				throw new NoConnectionException();
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
}
