package fredericosabino.fenixist;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import fredericosabino.fenixist.R;

public class ItemDescriptionFragment extends Fragment {
	Uri link;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		String description = getArguments().getString("description");
		String title = getArguments().getString("title");
		link = Uri.parse(getArguments().getString("link"));
		View view = inflater.inflate(R.layout.description_view, container, false);
		ActionBar bar = getActivity().getActionBar();
		/*bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayOptions(ActionBar.DISPLAY_USE_LOGO, ActionBar.DISPLAY_USE_LOGO);*/
		bar.setTitle(title);
		
		WebView web = (WebView) view.findViewById(R.id.webview);
		web.loadData(description, "text/html", null);
		
		//text.setText(description);
		return view;
	}
	
	public void onGlobeClick(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW, link);
		startActivity(intent);
	}
}
