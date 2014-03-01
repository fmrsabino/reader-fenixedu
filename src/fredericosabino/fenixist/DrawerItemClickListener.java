package fredericosabino.fenixist;

import java.util.ArrayList;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import fredericosabino.fenixist.R;

/**
 * Implementation of the drawer ListView listener and Fragment switcher for each item on the drawer
 * Requires the activity where the fragments will be shown to get the Fragment Manager
 */

public class DrawerItemClickListener implements ListView.OnItemClickListener {
	private MainActivity _activity;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ArrayList<String> _listTitles;
	
	public DrawerItemClickListener(MainActivity activity, ArrayList<String> listTitles) {
		_activity = activity;
		mDrawerList = (ListView) _activity.findViewById(R.id.left_drawer);
		mDrawerLayout = (DrawerLayout) _activity.findViewById(R.id.drawer_layout);
		_listTitles = listTitles; 
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		displayNews(position - mDrawerList.getHeaderViewsCount()); //because of the header for the list
	}
	
	@SuppressWarnings("unused")
	private void displayCalendar(int position) {
		Fragment calendar = new CalendarFragment();
		FragmentManager fragManager = _activity.getFragmentManager();
		
		
		fragManager.beginTransaction().replace(R.id.fragment_container, calendar).addToBackStack(null).commit();
		
		// Highlight the selected item, update the title, and close the drawer
	    mDrawerList.setItemChecked(position, true);
	    _activity.setTitle(_listTitles.get(position));
	    mDrawerLayout.closeDrawer(mDrawerList);
	}
	
	private void displayNews(int position) {
		Fragment newsFeed = new NewsFeedFragment();
		FragmentManager fragManager = _activity.getFragmentManager();
		
		Bundle args = new Bundle();
		args.putInt("drawerIndex", position);
		args.putStringArrayList("cNames", _activity.getCNames());
		args.putStringArrayList("cURL", _activity.getCURL());
		args.putStringArrayList("cHomePages", _activity.getCHomePages());
		newsFeed.setArguments(args);
		
		fragManager.beginTransaction().replace(R.id.fragment_container, newsFeed).commit();
		// Highlight the selected item, update the title, and close the drawer
	    mDrawerList.setItemChecked(position + mDrawerList.getHeaderViewsCount(), true); //because of the header for the list
	    _activity.setTitle(_listTitles.get(position));
	    mDrawerLayout.closeDrawer(mDrawerList);
	}
}
