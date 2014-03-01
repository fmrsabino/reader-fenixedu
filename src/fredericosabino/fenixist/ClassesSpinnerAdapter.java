package fredericosabino.fenixist;

import fredericosabino.fenixist.R;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class ClassesSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {
	private ArrayList<String> _classesNames;
	private ArrayList<String> _cUrl;
	private Context _context;
	
	public ClassesSpinnerAdapter(Context context, ArrayList<String> cNames, ArrayList<String> cUrl) {
		_classesNames = cNames;
		_cUrl = cUrl;
		_context = context;
	}

	@Override
	public int getCount() {
		return _classesNames.size();
	}

	@Override
	public Object getItem(int arg0) {
		return new ClassPair(_classesNames.get(arg0), _cUrl.get(arg0));
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		View view = LayoutInflater.from(_context).inflate(R.layout.actionbar_spinner_item, null);
		TextView text = (TextView) view.findViewById(R.id.text1);
		text.setText(_classesNames.get(arg0));
		return view;
	}
	
	/**
	 * No Pair in Java...
	 * */
	public class ClassPair {
		private String _className;
		private String _classURL;
		
		public ClassPair(String className, String classURL) {
			_className = className;
			_classURL = classURL;
		}

		public String getClassName() {
			return _className;
		}

		public String getClassURL() {
			return _classURL;
		}
	}

}
