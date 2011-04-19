package ca.uwaterloo.android.UWWeather;

import java.util.ArrayList;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UWWeatherActivity extends ListActivity implements AnimationListener {
    private static final String TAG = "UWWeather";

    private View mTitleBar;
    private ProgressBar mProgress;

    private Animation mSlideIn;
    private Animation mSlideOut;

    private TextView mTitle;
    private ArrayList<Pair<String,String>> mDetailList;

    private ListView mListView;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.currentweather);

    	Log.v(TAG,"Activity onCreate being called");

    	setTitle(getString(R.string.app_name_long));

    	// Load animations used to show/hide progress bar
        mSlideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        mSlideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);

        // Listen for the "in" animation so we make the progress bar visible
        // only after the sliding has finished.
        mSlideIn.setAnimationListener(this);

        mTitleBar = findViewById(R.id.title_bar);
        mTitle = (TextView) findViewById(R.id.title);
        mProgress = (ProgressBar) findViewById(R.id.progress);

        mListView = (ListView) findViewById(android.R.id.list);
        mDetailList = new ArrayList<Pair<String,String>>();

        WebFetch.prepareUserAgent(this);

        onNewIntent(getIntent());
    }

    // Start things moving. Entry point from statup and through the widget.
    private void startApp() {

    	// Start lookup for weather
        String url = getString(R.string.weather_url);
        new LookupTask().execute(url);
    }

    /**
     * Handle new incoming intents
     */
    @Override
    public void onNewIntent(Intent intent) {
    	startApp();
    }
   
    /**
     * Set the title for the current entry.
     */
/*    protected void setEntryTitle(String entryText) {
        mEntryTitle = entryText;
        mTitle.setText(mEntryTitle);
    }
*/
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.weathermenu, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh: {
            	// Just start again
                Intent defineIntent = getIntent(); //new Intent(this, UWWeatherActivity.class);
                startActivity(defineIntent);
//                finish();
                // TODO
                return true;
            }
            case R.id.menu_about: {
                showAbout();
                return true;
            }
        }
        return false;
    }

    /**
     * Show an about dialog that cites data sources.
     */
    protected void showAbout() {
        // Inflate the about message contents
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
        int defaultColor = textView.getTextColors().getDefaultColor();
        textView.setTextColor(defaultColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setIcon(R.drawable.app_icon);
        builder.setTitle(R.string.app_name_long);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }
  
    /**
     * Set the content for the current entry. This will update our
     * {@link WebView} to show the requested content.
     */
    protected void setEntryContent(String entryContent)
    	throws parseWeatherXML.ParseException {
    	
    	// Set up a parser to fish out all the details we need.
    	parseWeatherXML parser = new parseWeatherXML(entryContent);
    	Map<String, String> parsed = parser.parse();	// throws ParseException

    	String content = "";
    	String label = "";
    	Pair<String,String> pair;
    	
    	// Fill in the activity title and details.
    	content = String.format("%sh%s, %s %s %s",
    			parsed.get("observation_hour"), parsed.get("observation_minute"),
    			parsed.get("observation_day"),
    			parsed.get("observation_month_text"),
    			parsed.get("observation_year"));
    	mTitle.setText(content);
    	
    	// Check that the data is current
       	int h = Integer.decode(parsed.get("observation_hour"));
    	int m = Integer.decode(parsed.get("observation_minute"));
    	Time t = new Time();
    	t.setToNow();
    	int expectedH = t.hour;
    	if (t.isDst == 1) {	// weather station ignores DST, so need to adjust
    		if (expectedH == 0)
    			expectedH = 23;
    		else
    			expectedH -= 1;
    	}
    	int expectedM = t.minute - (t.minute % 15);
    	if (h != expectedH || m != expectedM)
    		mTitle.setTextColor(0xFFFF0000);
    	
		content = String.format("%s C", parsed.get("temperature_current_C"));
    	label = parsed.get("humidex_C");
    	if (label.equals("N_A"))
    		label = parsed.get("windchill_C");
    	if (!label.equals("N_A"))
    		content += String.format("   [%s C]", label);
		pair = new Pair<String,String>("Temperature", content);
		mDetailList.add(pair);
		
		content = String.format("%s C / %s C", parsed.get("temperature_24hrmin_C"), parsed.get("temperature_24hrmax_C"));
		pair = new Pair<String,String>("24hr min / max", content);
		mDetailList.add(pair);

		content = "";
		label = "Precipitation (";
		if (!parsed.get("precipitation_15minutes_mm").equals("N_A")) {
			label += "15min/";
			content += parsed.get("precipitation_15minutes_mm") + " / ";
		}
		if (!parsed.get("precipitation_1hr_mm").equals("N_A")) {
			label += "1hr";
			content += parsed.get("precipitation_1hr_mm");
		}
		if (!parsed.get("precipitation_24hr_mm").equals("N_A")) {
			label += "/24hr";
			content += " / " + parsed.get("precipitation_24hr_mm");
		}
		label += ")";
		content += " mm";
		pair = new Pair<String,String>(label,content);
		mDetailList.add(pair);
		
		content = String.format("%s %% / %s C", parsed.get("relative_humidity_percent"), parsed.get("dew_point_C"));
		pair = new Pair<String,String>("R.Humidity / Dew point", content);
		mDetailList.add(pair);
		
		content = String.format("%s km/h %s", parsed.get("wind_speed_kph"), parsed.get("wind_direction"));
		pair = new Pair<String,String>("Wind Speed / Direction", content);
		mDetailList.add(pair);

		content = String.format("%s kPa %s", parsed.get("pressure_kpa"), parsed.get("pressure_trend"));
		pair = new Pair<String,String>("Barometric Pressure", content);
		mDetailList.add(pair);

		content = String.format("%s W/m2", parsed.get("incoming_shortwave_radiation_WM2"));
		pair = new Pair<String,String>("Incoming Radiation", content);
		mDetailList.add(pair);

		LayoutInflater inflater = getLayoutInflater();
		View rowView = inflater.inflate(R.layout.widget_message, null, true);
		TextView header = (TextView) rowView.findViewById(R.id.message);
		header.setText(parsed.get("credit"));
//		header.setHeight(40);
//		header.setBackgroundColor(0xff0000);
		header.setTextColor(0xf0aaaa00);
		mListView.setFooterDividersEnabled(true);
//		mListView.addFooterView(header);
		
        // Make the view transparent to show background
//        mListView.setBackgroundColor(0);
//        mListView.setBackgroundColor(0xFFFFFFFF);

    	setListAdapter(new ListArrayAdapter(this, mDetailList));
    }
    
    /**
     * Background task to handle Wiktionary lookups. This correctly shows and
     * hides the loading animation from the GUI thread before starting a
     * background query to the Wiktionary API. When finished, it transitions
     * back to the GUI thread where it updates with the newly-found entry.
     */
    private class LookupTask extends AsyncTask<String, String, String> {
        /**
         * Before jumping into background thread, start sliding in the
         * {@link ProgressBar}. We'll only show it once the animation finishes.
         */
        @Override
        protected void onPreExecute() {
            mTitleBar.startAnimation(mSlideIn);
        }

        /**
         * Perform the background query using {@link GetWeathe}, which
         * may return an error message as the result.
         */
        @Override
        protected String doInBackground(String... args) {
            String query = args[0];
            String pageText = null;

            try {
        		publishProgress(query);
        		
        		// TODO testing
//            	pageText = getString(R.string.uwweather);
//            	if (pageText.length() == 0)
            		pageText = WebFetch.getUrlContent(query);

            } catch (WebFetch.ApiException e) {
                Log.e(TAG, "Problem making weather http request", e);
                pageText = null;
            }

            return pageText;
        }

        /**
         * When finished, push the newly-found entry content into our
	     * {@link WebView} and hide the {@link ProgressBar}.
	     */
	    @Override
	    protected void onPostExecute(String pageText) {
	        mTitleBar.startAnimation(mSlideOut);
	        mProgress.setVisibility(View.INVISIBLE);
	
	        if (pageText != null) {
	        	try {
	        		setEntryContent(pageText);
	            } catch (parseWeatherXML.ParseException e) {
	            	Log.e(TAG, "Problems parsing weather text", e);
	            	pageText = null;
	            }
	        }
	        
            if (pageText == null)
            	mTitle.setText(getString(R.string.empty_result));
	    }
	}

    /**
     * Make the {@link ProgressBar} visible when our in-animation finishes.
     */
    public void onAnimationEnd(Animation animation) {
        mProgress.setVisibility(View.VISIBLE);
    }

    public void onAnimationRepeat(Animation animation) {
        // Not interested if the animation repeats
    }

    public void onAnimationStart(Animation animation) {
        // Not interested when the animation starts
    }

}