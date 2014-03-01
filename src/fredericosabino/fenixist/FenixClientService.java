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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParserException;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

/**
 * Service that creates a worker thread to to handle all start requests, on at a time
 **/
public class FenixClientService extends Service {
	private String TAG = "FENIX_SERVICE";
	private ArrayList<String> cNames;
	private ArrayList<String> _feedsURL; //received in the service intent
	private ArrayList<ArrayList<RSSItem>> _newFeed;
	private ArrayList<ArrayList<RSSItem>> _oldFeed;
	private ArrayList<ArrayList<RSSItem>> _news;
	private Thread thread; //currently running thread
	
	private Handler handler = new Handler();
	
	private void sendNewsNotification(ArrayList<ArrayList<RSSItem>> news) {
		int mID = 0; //represents the position of the course in the array. If there are any more news it stacks up with the correct notification
		Log.i(TAG, "Creating Notification");
		long[] pattern = {100, 1000};
		ArrayList<NotificationCompat.Builder> notificationBuilders = new ArrayList<NotificationCompat.Builder>();
		
		for(mID = 0; mID < news.size(); ++mID) {
				
				notificationBuilders.add(new NotificationCompat.Builder(this)
				.setAutoCancel(true)
				.setSmallIcon(R.drawable.ic_stat_ist_logo)
				.setContentTitle(cNames.get(mID))
				.setContentText("New announcements")
				.setLights(0x29EAFF, 500, 2000)
				.setVibrate(pattern))
				;
		}

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		for(mID = 0; mID < news.size(); ++mID) {
			if(!news.get(mID).isEmpty()) {
				Intent intent = new Intent(this, MainActivity.class);
				intent.putExtra("notificationIndex", mID);
				
				// The stack builder object will contain an artificial back stack for the
				// started Activity.
				// This ensures that navigating backward from the Activity leads out of
				// your application to the Home screen.
				TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
				// Adds the back stack for the Intent (but not the Intent itself)
				stackBuilder.addParentStack(MainActivity.class);
				// Adds the Intent that starts the Activity to the top of the stack
				stackBuilder.addNextIntent(intent);
				PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(mID, PendingIntent.FLAG_UPDATE_CURRENT);
				notificationBuilders.get(mID).setContentIntent(resultPendingIntent);
				
				mNotificationManager.notify(mID, notificationBuilders.get(mID).build());
			}
		}	
	}
	
	private Runnable updateNews = new Runnable() {
		@Override
		public void run() {
			if(_feedsURL != null && cNames != null) {
				thread = performOnBackgroundThread(new FeedDownloaderThread(_feedsURL));
			}
			handler.postDelayed(this, 480000); //8 minutes
		}
	};
	
	private class FeedDownloaderThread implements Runnable {
		private ArrayList<String> _urls;
		private RSSParser _parser;
		
		FeedDownloaderThread(ArrayList<String> URL) {
			_urls = URL;
			try {
				_parser = new RSSParser();
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			handleNews(_urls, _parser);
		}
	}
	
	public static Thread performOnBackgroundThread(final Runnable runnable) {
	    final Thread t = new Thread() {
	        @Override
	        public void run() {
	            try {
	                runnable.run();
	            } finally {

	            }
	        }
	    };
	    t.start();
	    return t;
	}
	
	@SuppressWarnings("resource")
	static String convertStreamToString(java.io.InputStream is) {
	    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	

	private synchronized void  handleNews(ArrayList<String> urls, RSSParser parser) {
		Log.i(TAG, "Handling Message");
		ArrayList<RSSItem> result = null;
		
		//refresh the contents of the old feed. The user may have already seen the news in the app
		loadOldFeed();
		
		ArrayList<Boolean> toUpdate = checkNews(urls);
		
		//Get the new feed for each course the user is attending and store in the _newFeed
		
			_newFeed = new ArrayList<ArrayList<RSSItem>>();
			
			for(int i = 0; i < urls.size(); i++) {
				if(toUpdate.get(i) == true) { //new items
					Log.i(TAG, "New updates from " + cNames.get(i));
					try {
						ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
						NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
						if(networkInfo != null && networkInfo.isConnected()) {
							result = parser.parse(downloadFeed(urls.get(i)));
						} else {
							return; //lost connection try again later TODO: may not be this now...
						}
						_newFeed.add(result);
					} catch (XmlPullParserException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					_newFeed.add(_oldFeed.get(i)); //adds the old items
				}
			}
			
			//We compare the feed element by element to check if there is really news after the content size change
			Log.i(TAG, "Comparing Feeds");
			_news = compareFeeds(_oldFeed, _newFeed);
			_oldFeed = new ArrayList<ArrayList<RSSItem>>(_newFeed);
			
			boolean weHaveNews = false;
			for(int i = 0; i < _news.size(); ++i) {
				ArrayList<RSSItem> item = _news.get(i);
				if(!item.isEmpty()) {
					weHaveNews = true;
					try {
						FileOutputStream fos = openFileOutput(cNames.get(i), Context.MODE_PRIVATE);
						ObjectOutputStream oos = new ObjectOutputStream(fos);
						oos.writeObject(_newFeed.get(i));
						oos.flush();
						fos.close();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			_newFeed = null;
			if(weHaveNews) {
				sendNewsNotification(_news);
			}
	}
	
	/*
	 * Cannot be used by the main thread!
	 * Downloads the HTTP headers of the feeds and tells if there is new stuff by comparing the Content-Length field
	 **/
	private ArrayList<Boolean> checkNews(ArrayList<String> urls) {
		ArrayList<Boolean> result;
		ArrayList<Long> sizes = loadFeedSizes();
		if(sizes == null) {
			return null; //the file doesn't exist
		} else {
			result = new ArrayList<Boolean>();
			for(int i = 0; i < urls.size(); i++) {
				try {					
					AndroidHttpClient httpclient = AndroidHttpClient.newInstance("", this);
				    HttpHead httphead = new HttpHead(urls.get(i));
				    HttpResponse response = httpclient.execute(httphead);
				    httpclient.close();
				    
				    long length = Long.valueOf(response.getFirstHeader("Content-Length").getValue());
				    Log.i(TAG, "Content length of " + cNames.get(i) + " is " + length);
					
					if(length != sizes.get(i)) {
						result.add(true); //new content
						sizes.set(i, length);
					} else {
						result.add(false); //no new content
					}
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			writeFeedSizes(sizes);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<Long> loadFeedSizes() {
		File file = getFileStreamPath("feedSizes");
		ArrayList<Long> result = null;
		if(file.exists()) {
			try {
				FileInputStream fis = openFileInput("feedSizes");
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
			FileOutputStream fos = openFileOutput("feedSizes", Context.MODE_PRIVATE);
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
	
	/**
	 * Loads old feed from storage
	 * If some feed has no items/hasn't been loaded it'll be detected when comparing the feeds to check the news
	 **/
	@SuppressWarnings("unchecked")
	public void loadOldFeed() {
		_oldFeed = new ArrayList<ArrayList<RSSItem>>();
		for(int i = 0; i < cNames.size(); ++i) {
			_oldFeed.add(new ArrayList<RSSItem>());
		}
		for(int i = 0; i < cNames.size(); ++i) {
			File file = getFileStreamPath(cNames.get(i));
			FileInputStream fis = null;
			ObjectInputStream ois = null;
			ArrayList<RSSItem> result;
			if(file.exists()) {
				try {
					fis = openFileInput(cNames.get(i));
					ois = new ObjectInputStream(fis);
					result = (ArrayList<RSSItem>) ois.readObject();
					_oldFeed.set(i, result);
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
			}
		}
	}
	
	/**
	 * Update only the URL references
	 * */
	@SuppressWarnings("unchecked")
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        
        try {
			fis = openFileInput("cURL");
			ois = new ObjectInputStream(fis);
			_feedsURL = (ArrayList<String>)ois.readObject();
			
			fis = openFileInput("cNames");
			ois = new ObjectInputStream(fis);
			cNames = (ArrayList<String>)ois.readObject();
			loadOldFeed();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "onStartCommand: File not found!");
			stopSelf();
		} catch (StreamCorruptedException e) {
			Log.e(TAG, "Stream Corrupted!");
			stopSelf();
		} catch (IOException e) {
			Log.e(TAG, "onStartCommand: IOException!");
			stopSelf();
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "onStartCommand: ClassNotFoundException!");
			stopSelf();
		} finally {
			try {
				if(ois != null)
					ois.close();
				if(fis != null)
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
        
        //Remove Pending posts
      	handler.removeCallbacks(updateNews);
      	handler.postDelayed(updateNews, 10000); //10 seconds to start the service job
        return START_STICKY;
    }
	
	
	@Override
	public void onCreate() {
		Log.i(TAG, "Service created!");
	}
	
	/**
	 * Receives 2 feeds with RSSItems and compares each feed.
	 * Returns an ArrayList for each different course with the new items.
	 * WARNING: If a specific array is empty it means there are no new items for that specific course
	 * */
	public ArrayList<ArrayList<RSSItem>> compareFeeds(ArrayList<ArrayList<RSSItem>> oldFeed, ArrayList<ArrayList<RSSItem>> newFeed) {
		ArrayList<ArrayList<RSSItem>> newItems = new ArrayList<ArrayList<RSSItem>>();
		for(int i = 0; i < oldFeed.size(); ++i) {
			newItems.add(new ArrayList<RSSItem>());
		}
		
		Log.i(TAG, "" + oldFeed.size());
		
		for(int i = 0; i < oldFeed.size(); i++) { //for each course
			if(oldFeed.get(i).isEmpty()) { //the course feed has no items
				if(newFeed.get(i).isEmpty()) //and continues to have no items = no news
					continue;
				else { //we have news that we hadn't so all the new feed goes to the newItems for the notification
					newItems.set(i, newFeed.get(i));
					continue;
				}
			}
			RSSItem oldItem = oldFeed.get(i).get(0); //same old item for the comparison
			for(int j = 0; j < newFeed.get(i).size(); j++) {//for each item of that course
				RSSItem newItem = newFeed.get(i).get(j);
				if(!oldItem.get_guid().equals(newItem.get_guid())) {
					//add the new item
					newItems.get(i).add(newItem);
				} else {
					break; //if the items are the same it means that there is no more news and we move to the next
				}
				
			}
		}
		return newItems;
	}
	
	@Override
	public void onDestroy() {
		if(thread != null) {
			thread.interrupt();
		}
		handler.removeCallbacks(updateNews);
		super.onDestroy();
	}
	
	private InputStream downloadFeed(String url) throws IOException {		
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		
		return entity.getContent();
		//return connection.getInputStream();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		//doesn't allow binding
		return null;
	}
}
