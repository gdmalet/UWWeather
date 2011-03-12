/**
 * An adapter that is used when drawing the main window list of details.
 */
package ca.uwaterloo.android.UWWeather;

import java.util.ArrayList;

import android.app.Activity;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * @author gdmalet
 *
 */
public class ListArrayAdapter extends ArrayAdapter<Pair<String,String>> {
	private final Activity context;
	private final ArrayList<Pair<String,String>> details;

	public ListArrayAdapter(Activity context, ArrayList<Pair<String,String>> details) {
		super(context, R.layout.rowlayout, details);
		this.context = context;
		this.details = details;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.rowlayout, null, true);

		String str;
		
		TextView label = (TextView) rowView.findViewById(R.id.label);
		str = details.get(position).first;
		label.setText(str);

		TextView value = (TextView) rowView.findViewById(R.id.value);
		str = details.get(position).second;
		value.setText(str);
		
		return rowView;
	}
}
