package fredericosabino.fenixist;

import fredericosabino.fenixist.R;
import java.util.ArrayList;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NewsListAdapter extends BaseAdapter{
	private Context _activity;
	private ArrayList<RSSItem> _items;

	NewsListAdapter(Context activity, ArrayList<RSSItem> items) {
		_activity = activity;
		_items = items;
	}
	
	@Override
	public int getCount() {
		return _items.size();
	}

	@Override
	public Object getItem(int arg0) {
		return _items.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		View view = LayoutInflater.from(_activity).inflate(R.layout.news_list_item, null);
		TextView title = (TextView)view.findViewById(R.id.newsTitle);
		title.setText(_items.get(arg0).get_title());
		TextView description = (TextView)view.findViewById(R.id.newsDescription);
		description.setText(Html.fromHtml(_items.get(arg0).get_description()));
		TextView pubDate = (TextView)view.findViewById(R.id.newsPubDate);
		pubDate.setText(_items.get(arg0).get_pubDate());
		return view;
	}
}
