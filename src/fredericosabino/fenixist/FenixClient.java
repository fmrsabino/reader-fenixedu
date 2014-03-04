package fredericosabino.fenixist;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
	
	@SuppressWarnings("resource")
	static String convertStreamToString(java.io.InputStream is) {
	    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
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
	    String result = null;
	    InputStream in = null;
	    HttpURLConnection urlConnection = null;
	    //Download
		try {
			//Check Internet connection
			ConnectivityManager connMgr = (ConnectivityManager) _activity.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if(networkInfo != null && networkInfo.isConnected()) {
				URL url2 = new URL(url);
			    urlConnection = (HttpURLConnection) url2.openConnection();
			    in = new BufferedInputStream(urlConnection.getInputStream());
			    result = convertStreamToString(in);
			} else {
				throw new NoConnectionException();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(urlConnection != null) {
					urlConnection.disconnect();
				}
				if(in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
}
