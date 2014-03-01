package fredericosabino.fenixist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import fredericosabino.fenixist.R;
import fredericosabino.fenixist.exceptions.NoConnectionException;
import fredericosabino.fenixist.fenixdata.CourseInfo;
import fredericosabino.fenixist.fenixdata.UserCoursesInfo;
import fredericosabino.fenixist.fenixdata.UserCoursesInfo.Enrolment;

public class MainActivity extends Activity {
	public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private DrawerLayout mDrawerLayout;
	
	private ArrayList<String> cNames;
	private ArrayList<String> cURL;
	private ArrayList<String> cHomePages;
	
	protected int feedIndex = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Log.i("ACTIVITY", "On Create.");
		try {
			setupClasses();
			serviceStart();
			setupUI();
		} catch (NoConnectionException e) {
			//If we don't have an Internet connection retrieve from storage
			boolean filesFound = false;
			File file = getFileStreamPath("cURL");
			if(file.exists()) {
				loadURLSFromStorage();
				file = getFileStreamPath("cNames");
				if(file.exists()) {
					loadCNamesFromStorage();
					file = getFileStreamPath("HomePages");
					if(file.exists()) {
						loadHomePagesFromStorage();
						filesFound = true;
					}
				}
			}
			Log.i("MainActivity", "Files Found = " + filesFound);
			if(filesFound == false) {
				//no files no connection... display a message to retry. We cannot even start the service!
				setContentView(R.layout.no_connection);
			} else {
				//setup only the UI. The service starts automatically when we have Internet connection
				setupUI();
			}
		}
	}
	
	public void retryConnection(View view) {
		Log.i("MainActivity", "Starting retryConnection");
		View text = findViewById(R.id.noConnection);
		View button = findViewById(R.id.retryConnection);
		View progress = findViewById(R.id.progressBar1);
		//Check Internet Connection
				ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
				if(networkInfo != null && networkInfo.isConnected()) {
					try {
						text.setVisibility(View.GONE);
						button.setVisibility(View.GONE);
						progress.setVisibility(View.VISIBLE);
						setupClasses();
						setupUI();
					} catch (NoConnectionException e) {
						progress.setVisibility(View.GONE);
						button.setVisibility(View.VISIBLE);
						text.setVisibility(View.VISIBLE);
					}
				} else {
					//make toast
				}
		
	}
	
	/*
	 * Setups the activity UI. Also makes the News Fragment the initial displayed fragment
	 **/
	private void setupUI() {
		setContentView(R.layout.activity_main);
		Fragment newsFrag = new NewsFeedFragment();
		FragmentManager fragMan = getFragmentManager();
		
		//Received the index from the service when the user click the notification
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		if(bundle == null) {
			bundle = new Bundle();
		}
		bundle.putStringArrayList("cNames", cNames);
		bundle.putStringArrayList("cURL", cURL);
		bundle.putStringArrayList("cHomePages", cHomePages);
		
		newsFrag.setArguments(bundle);
		
		setupNavigationDrawer();
		
		fragMan.beginTransaction().replace(R.id.fragment_container, newsFrag).commit();
	}
	
	private void setupNavigationDrawer() {
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout); 
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		
		// enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
		
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
			/** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
            	getActionBar().setTitle(cNames.get(feedIndex));
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
            	getActionBar().setTitle("Fenix@IST");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
		};
		
		// Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        
		TextView listHeader = new TextView(this);
		listHeader.setText("Enrolments");
		listHeader.setEnabled(false);
		listHeader.setOnClickListener(null);
		mDrawerList.addHeaderView(listHeader);
		
		//Set the adapter for the list view
		mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, cNames));
		//Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener(this, cNames));
		//News Fragment already selected
		mDrawerList.setItemChecked(0, true);
	}
	
	/* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        MenuItem refresh = menu.findItem(R.id.action_refresh);
        if(refresh != null) {
        	refresh.setVisible(!drawerOpen);
        }
        MenuItem courseHome =  menu.findItem(R.id.action_course_home);
        if(courseHome != null) {
        	courseHome.setVisible(!drawerOpen);
        }
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
       if (mDrawerToggle.onOptionsItemSelected(item)) {
           return true;
       }
		return super.onOptionsItemSelected(item);
	}
	
	private void setupClasses() throws NoConnectionException {
		FenixClient fClient = new FenixClient(this);
		cNames = new ArrayList<String>();
		cURL = new ArrayList<String>();
		cHomePages = new ArrayList<String>();
		
		ArrayList<ClassData> cds = new ArrayList<MainActivity.ClassData>();
		UserCoursesInfo courses = fClient.getUserCourses();
		for(Enrolment e : courses.getEnrolments()) {
			Log.v("Enrolment", e.getName());
			CourseInfo course = fClient.getCourseInfo(e.getId());
			
			cds.add(new ClassData(e.getName(), course.getAnnouncementLink(), course.getUrl()));
		}
		
		//Sort lexicographically
		Collections.sort(cds, new ClassDataComparator());
		for(ClassData cd : cds) {
			cNames.add(cd.getName());
			cURL.add(cd.getUrl());
			cHomePages.add(cd.getHomePage());
		}
		
		//Check if we have the URL's in internal storage. The service is going to read from it
		writeURLSToStorage();
		writeCNamesToStorage();
		writeHomePagesToStorage();
	}
	
	private class ClassDataComparator implements Comparator<ClassData> {
		@Override
		public int compare(ClassData arg0, ClassData arg1) {
			return arg0.getName().compareToIgnoreCase(arg1.getName());
		}
		
	}
	
	private class ClassData {
		private String name;
		private String url;
		private String homePage;
		ClassData(String name, String url, String homePage) {
			this.name = name;
			this.url = url;
			this.homePage = homePage;
		}
		public String getName() {
			return name;
		}
		public String getUrl() {
			return url;
		}
		public String getHomePage() {
			return homePage;
		}
	}
	
	//FenixService reads from storage
	private boolean writeURLSToStorage() {
		try {
			FileOutputStream fos = openFileOutput("cURL", Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(cURL);
			return true;
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return false;
	}
	
	public void serviceStart() {
		/* Check if we the file with the feed sizes is created and initialized */
		File file = getFileStreamPath("feedSizes");
		if(!file.exists()) {
			setupFeedSizes();
		}
		Intent intent = new Intent(this, FenixClientService.class);
		startService(intent);
	}
	
	public boolean setupFeedSizes() {
		ArrayList<Long> sizes = new ArrayList<Long>();
		for(int i = 0; i < cNames.size(); i++){
			sizes.add((long) 0);
		}
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
	
	//FenixService reads from storage
	private boolean writeCNamesToStorage() {
		try {
			FileOutputStream fos = openFileOutput("cNames", Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(cNames);
			return true;
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return false;
	}
	
	private boolean writeHomePagesToStorage() {
		try {
			FileOutputStream fos = openFileOutput("HomePages", Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(cHomePages);
			return true;
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private boolean loadURLSFromStorage() {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = openFileInput("cURL");
			ois = new ObjectInputStream(fis);
			cURL = (ArrayList<String>) ois.readObject();
			return true;
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
		} finally {
			try {
				if(ois != null)
					ois.close();
				if(fis != null)
					fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private boolean loadCNamesFromStorage() {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = openFileInput("cNames");
			ois = new ObjectInputStream(fis);
			cNames = (ArrayList<String>) ois.readObject();
			return true;
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
		} finally {
			try {
				if(ois != null)
					ois.close();
				if(fis != null)
					fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private boolean loadHomePagesFromStorage() {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = openFileInput("HomePages");
			ois = new ObjectInputStream(fis);
			cHomePages = (ArrayList<String>) ois.readObject();
			return true;
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
		} finally {
			try {
				if(ois != null)
					ois.close();
				if(fis != null)
					fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		return false;
	}
	
	public ArrayList<String> getCNames() {
		return cNames;
	}
	
	public ArrayList<String> getCURL() {
		return cURL;
	}
	
	public ArrayList<String> getCHomePages() {
		return cHomePages;
	}
	
	/**
	 * Handles the click when the Web click when the view is the description fragment
	 **/
	public void onGlobeClick(View view) {
		ItemDescriptionFragment frag = (ItemDescriptionFragment)getFragmentManager().findFragmentByTag("DESCRIPTION_FRAG");
		if(frag.isVisible()) {
			frag.onGlobeClick(view);
		}
	}
	
	public static class ConnectionWatcher extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Checking Internet Connection
			ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			Log.i("ConnectionWatcher", "Received broadcast...");
			if(networkInfo != null && networkInfo.isConnected()) {
				Log.i("ConnectionWatcher", "Connected to the internet. Starting service.");
				context.startService(new Intent(context, FenixClientService.class));
			} else {
				Log.i("ConnectionWatcher", "Not connected to the internet. Stopping service.");
				context.stopService(new Intent(context, FenixClientService.class));
			}
		}
	}
}