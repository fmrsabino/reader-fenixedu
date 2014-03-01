package fredericosabino.fenixist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;



/**
 * TODO: implement a read status of the RSS feed
 * Each RSS item uses a unique identifier (guid)
 **/
public class NewsFeedFragment extends Fragment {
	private RSSParser parser;
	private static final String TAG = "Fenix@IST"; //debugging purposes ex.: Log.v(TAG, "index=" + i);
	private ArrayList<RSSItem> _items; //needed for the click listener
	private int _activeIndex = 0; //Keep the Index in the bundle for fragment restoration
	
	private ArrayList<String> cURL; //announcement board link
	private ArrayList<String> cNames;
	private ArrayList<String> cHomePages;
	
	private String cHomePage; //current displayed course Homepage
	
	public boolean loadNewsFromFile(int posistion) {
		try {
			FileInputStream fis = getActivity().openFileInput(cNames.get(posistion));
			ObjectInputStream ois = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			ArrayList<RSSItem> items = (ArrayList<RSSItem>)ois.readObject();
			showNews(items);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true); //Report that this fragment would like to participate in populating the options menu :)
		
		Log.i("FRAGMENT", "On CreateView.");
		//Received when the user clicks the notification handled by the service
		if(getArguments() != null) {
			Bundle bundle = getArguments();
			
			if(bundle.containsKey("notificationIndex")) {
				int notificationIndex = bundle.getInt("notificationIndex");
				Log.i(TAG, "Notification index received is: " + notificationIndex);
				_activeIndex = notificationIndex;
			}
			
			if(bundle.containsKey("drawerIndex")) {
				_activeIndex = bundle.getInt("drawerIndex");
			}
			((MainActivity)getActivity()).feedIndex = _activeIndex;
			cNames = bundle.getStringArrayList("cNames");
			cURL = bundle.getStringArrayList("cURL");
			cHomePages = bundle.getStringArrayList("cHomePages");
		}
		getActivity().getActionBar().setTitle(cNames.get(_activeIndex));
		try {
			parser = new RSSParser();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		cHomePage = cHomePages.get(_activeIndex);
		
		return inflater.inflate(R.layout.news_view, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		View rootView = getView();
		rootView.findViewById(R.id.news).setVisibility(View.GONE);
		rootView.findViewById(R.id.loadingFeed).setVisibility(View.VISIBLE);
		loadFeed();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("drawerIndex", _activeIndex);
	}
	
	public void loadFeed() {
		File file = getActivity().getFileStreamPath(cNames.get(_activeIndex));
		
		if(file.exists()) {
			Log.i(TAG, "File exists. Reading from file.");
			loadNewsFromFile(_activeIndex);
		} else {
			Log.i(TAG, "File doesn't exist. Downloading file.");
			
			//Checking Internet Connection
			ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if(networkInfo != null && networkInfo.isConnected()) {
				new DownloadFeed(_activeIndex).execute(cURL.get(_activeIndex));
			} else {
				//Display Connection error and give the option to retry
			}
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.news_feed_actions, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_refresh) {
			//Checking Internet Connection
			ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if(networkInfo != null && networkInfo.isConnected()) {
				getView().findViewById(R.id.news).setVisibility(View.GONE);
				View noItemsWarning = getView().findViewById(R.id.noItems);
				noItemsWarning.setVisibility(View.GONE);
				ProgressBar progress = (ProgressBar) getView().findViewById(R.id.loadingFeed);
				progress.setVisibility(View.VISIBLE);
				new DownloadFeed(_activeIndex).execute(cURL.get(_activeIndex));
			} else {
				makeToast("No Internet connection");
				Log.w(TAG, "No Internet Connection!");
			}
		}
		if(itemId == R.id.action_course_home) {
			Uri link = Uri.parse(cHomePage);
			Intent intent = new Intent(Intent.ACTION_VIEW, link);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void makeToast(String text) {
		Context context = getActivity().getApplicationContext();
		int duration = Toast.LENGTH_LONG;
		
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
	
	
	//Needed to all the downloads in this class to correct the exception caused by running the connection on the main thread.
	private class DownloadFeed extends AsyncTask<String, Void, ArrayList<RSSItem> > {
		private int index; //the index that was present when this task was created
		
		DownloadFeed(int index) {
			this.index = index;
		}
		
		@Override
		protected ArrayList<RSSItem> doInBackground(String... arg0) {
			_items = fillFeed(arg0[0], index);
			return _items;
		}
		
		
		@Override
		protected void onPostExecute(ArrayList<RSSItem> result) {
			//write to file with the same name as the course
			try {
				Log.i(TAG, "Writing news to file.");
				FileOutputStream fos = getActivity().openFileOutput(cNames.get(index), Context.MODE_PRIVATE);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(result);
				oos.flush();
				fos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Show the news
			showNews(result);
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			Log.i(TAG, "Task cancelled. No news.");
			loadNewsFromFile(index);
		}
		
		/**
		 * Receives an url and downloads the contents from the webserver specified by this url
		 */
		public InputStream getFeed(String url, int index, AndroidHttpClient httpclient) throws IOException, MalformedURLException {
		    HttpHead httphead = new HttpHead(url);
		    Log.i(TAG, "Method is: " + httphead.getMethod());
		    HttpResponse response = httpclient.execute(httphead);
		    long size = Long.valueOf(response.getFirstHeader("Content-Length").getValue());
		    Log.i(TAG, "Content length of " + cNames.get(index) + " is " + size);
			
			ArrayList<Long> sizes = loadFeedSizes();
			if(sizes.get(index).equals(size)) { //we don't have anything new... cancel the task
				httpclient.close();
				cancel(true);
				return null;
			}
			//from this point we know that we have news so we need to download the feed
			sizes.set(index, size);
			writeFeedSizes(sizes);
			httpclient.close();
			
			httpclient = AndroidHttpClient.newInstance("", getActivity());
			HttpGet httpget = new HttpGet(url);
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			InputStream is = entity.getContent();
			
			return is;
		}
		
		/**
		 * Called while executing the AsyncThread in the Background
		 */
		public ArrayList<RSSItem> fillFeed(String url, int index) {
			InputStream stream = null;
			ArrayList<RSSItem> feed = new ArrayList<RSSItem>();
			
			try {
				//Download the feed from URL
				AndroidHttpClient httpclient = AndroidHttpClient.newInstance("", getActivity());
				stream = getFeed(url, index, httpclient);
				httpclient.close();
				if(stream == null) { //we don't have any news
					return null;
				}
				//Parse the feed present in the stream
				feed = parser.parse(stream);
			} catch (MalformedURLException e) {
				Log.v(TAG, "Exception: MalformedURLException");
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return feed;
		}
	}
	
	private void showNews(ArrayList<RSSItem> result) {
		View view = getView();
		ListView list = (ListView) view.findViewById(R.id.news);
		Activity act = getActivity(); //needed because ArrayAdapter needs a context (Activity)
		View loading = view.findViewById(R.id.loadingFeed);
		
		if(result.isEmpty()) {
			Log.w(TAG, "ArrayList is Empty!!");
			View noItemsWarning = view.findViewById(R.id.noItems);
			loading.setVisibility(View.GONE);
			noItemsWarning.setVisibility(View.VISIBLE);
			return;
		}
		_items = result;
		
		list.setAdapter(new NewsListAdapter(act, result));
		list.setOnItemClickListener(new NewsFeedClickListener());
		
		loading.setVisibility(View.GONE);
		View noItemsWarning = view.findViewById(R.id.noItems);
		noItemsWarning.setVisibility(View.GONE);
		list.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Click Listener for the titles present on the feed list
	 * Starts the transaction to ItemDescriptionFragment upon item selection
	 */
	private class NewsFeedClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			displayDescription(position);
		}
		
		public void displayDescription(int position) {
			Bundle args = new Bundle();
			String title = _items.get(position).get_title();
			String description = _items.get(position).get_description();
			String link = _items.get(position).get_link();
			args.putString("link", link);
			args.putString("description", description);
			args.putString("title", title);
			Fragment frag = new ItemDescriptionFragment();
			frag.setArguments(args);
			
			getFragmentManager().beginTransaction()
								.replace(R.id.fragment_container, frag, "DESCRIPTION_FRAG")
								.addToBackStack(null)
								.commit();
		}

	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<Long> loadFeedSizes() {
		File file = getActivity().getFileStreamPath("feedSizes");
		ArrayList<Long> result = null;
		if(file.exists()) {
			try {
				FileInputStream fis = getActivity().openFileInput("feedSizes");
				ObjectInputStream ois = new ObjectInputStream(fis);
				result = (ArrayList<Long>) ois.readObject();
				return result;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (StreamCorruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			return result;
		}
		return result;
	}
	
	private boolean writeFeedSizes(ArrayList<Long> sizes) {
		try {
			FileOutputStream fos = getActivity().openFileOutput("feedSizes", Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(sizes);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
