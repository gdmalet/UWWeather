/**
 * 
 */
package ca.uwaterloo.android.UWWeather;

import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.Log;
import android.util.LogPrinter;
import android.widget.RemoteViews;
import android.widget.Toast;
import ca.uwaterloo.android.UWWeather.WebFetch.ApiException;
import ca.uwaterloo.android.UWWeather.parseWeatherXML.ParseException;

/**
 * @author gdmalet
 *
 */
public class UWWeatherWidget extends AppWidgetProvider {
	private static final String TAG = "AppWidgerProvideer";
	/**
	 * Trigger at 5,20,35,50 past the hour,
	 * and repeat every 15 minutes.
	 */
	private int minutesUntilTrigger() {
		Time t = new Time();
    	t.setToNow();
    	return 15 - ((t.minute+10) % 15);	
	}
	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
    	Log.v(TAG, "onupdate being called");

    	// Set an alarm to process updates. Do this so the device doesn't
    	// wake up if it is asleep when this triggers.
    	AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent defineIntent = new Intent(context, UpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, defineIntent, 0);
    	long triggerAtTime = SystemClock.elapsedRealtime() + (minutesUntilTrigger() * 60*1000);
    	alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime, 15*60*1000, pendingIntent);
/*
    	// TODO -- need to deal with multiple instances
        final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
           int appWidgetId = appWidgetIds[i];
*/            
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends Service {
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
        	Log.v(TAG,"service onStartCommand");
        	
            // Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, UWWeatherWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);

            return START_NOT_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
        	Log.v(TAG,"service onBind");
            return null;
        }

        /**
         * Build a widget update to show the current weather.
         * Will block until the online API returns.
         */
        public RemoteViews buildUpdate(Context context) {
        	Log.v(TAG,"buildUpdate");

            Resources res = context.getResources();

            String url = res.getString(R.string.weather_url);
            String pageText = null;

        	parseWeatherXML parser = null;
        	Map<String, String> parsed = null;

        	try {
            	// Try fetch the current weather
                WebFetch.prepareUserAgent(context);

                // TODO testing
//            	pageText = res.getString(R.string.uwweather);
//            	if (pageText.length() == 0)
            		pageText = WebFetch.getUrlContent(url);

            	if (pageText != null) {
                	parser = new parseWeatherXML(pageText);
                	parsed = parser.parse();
            	}

            	Log.v(TAG,"fetched text");
            	
            } catch (ApiException e) {
                Log.e(TAG, "Problem making weather http request", e);
            	Toast.makeText(context,
            			"Problems fetching UW weather details: check your connection",
            			Toast.LENGTH_LONG);
                pageText = null;
            } catch (ParseException e) {
                Log.e("UWWeatherWidget", "Problems parsing weather text", e);
                pageText = null;
            }

            RemoteViews views = null;

        	if (pageText != null) {
            	
            	Log.v(TAG,"parsed text");

            	// Build an update that holds the updated widget contents
                views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);

            	String content = "";
            	String label = "";
            	
        		content = String.format("%sC", parsed.get("temperature_current_C"));
            	label = parsed.get("humidex_C");
            	if (label.equals("N_A"))
            		label = parsed.get("windchill_C");
            	if (!label.equals("N_A"))
            		content += String.format(" [%s]", label);
        		views.setTextViewText(R.id.weather_temp, content);

        		content = String.format("%s/%sC",
                		parsed.get("temperature_24hrmin_C"), parsed.get("temperature_24hrmax_C"));
        		views.setTextViewText(R.id.weather_minmaxC, content);

        		content = String.format("%s/%smm",
        				parsed.get("precipitation_1hr_mm"), parsed.get("precipitation_24hr_mm"));
        		views.setTextViewText(R.id.weather_precip, content);

   				content = String.format("  %s W/m2",
   						parsed.get("incoming_shortwave_radiation_WM2"));
        		views.setTextViewText(R.id.weather_insolation, content);
        		
        		content = String.format("%s km/h %s",
        				parsed.get("wind_speed_kph"), parsed.get("wind_direction"));
        		views.setTextViewText(R.id.weather_wind, content);

        		content = String.format("%s%%/%sC",
        				parsed.get("relative_humidity_percent"), parsed.get("dew_point_C"));
        		views.setTextViewText(R.id.weather_humidity, content);

        		// Check that the data is current
               	int h = Integer.decode(parsed.get("observation_hour"));
            	int m = Integer.decode(parsed.get("observation_minute"));
            	Time t = new Time();
            	t.setToNow();
            	int expectedH = t.hour;
            	int expectedM = t.minute - (t.minute % 15);
            	if (h != expectedH || m != expectedM)
            		views.setTextColor(R.id.weather_temp, 0xFFFF0000);
            	
        		// When user clicks on widget, launch to main app
                Intent defineIntent = new Intent(context, UWWeatherActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context,
                        0 /* no requestCode */, defineIntent, 0 /* no flags */);
                views.setOnClickPendingIntent(R.id.widget, pendingIntent);	

            } else {
                // Didn't find weather, so show error message
                views = new RemoteViews(context.getPackageName(), R.layout.widget_message);
                views.setTextViewText(R.id.message, context.getString(R.string.widget_error));
            }
            return views;
        }
    }
}
