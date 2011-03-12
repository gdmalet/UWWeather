/**
 * Parse the string of weather data into a map of entries.
 */
package ca.uwaterloo.android.UWWeather;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Xml;

public class parseWeatherXML {

	private String mWeather;

    /**
     * Thrown when there were problems parsing the response to an API call,
     * either because the response was empty, or it was malformed.
     */
    public static class ParseException extends Exception {
    	static final long serialVersionUID = 0;
        public ParseException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }

	public parseWeatherXML(String weatherXML) {
		mWeather = weatherXML;
	}

    public Map<String, String> parse() throws ParseException {

        final Map<String, String> map = new HashMap<String,String>();

        class handler extends DefaultHandler {
        	private StringBuilder buildstr;

        	@Override
            public void startDocument() throws SAXException {
                super.startDocument();
                buildstr = new StringBuilder();
            }
      	
        	// Build up a string containing the label.
            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                super.characters(ch, start, length);
                
                // Drop leading newlines.
                if (buildstr.length() == 0 && ch[0] == '\n')
                	buildstr.append(ch, start+1, length-1);
                else
                	buildstr.append(ch, start, length);
            }
            
            // Have the label; this gives us the value.
            @Override
        	public void endElement(String uri, String localName, String qName) throws SAXException {
        		super.endElement(uri, localName, qName);
        		String value = buildstr.toString();
        		map.put(localName, value.trim());
                buildstr.setLength(0);
        	}
        }
        
        try {
            Xml.parse(mWeather, new handler());
        } catch (Exception e) {
            throw new ParseException("Problems parsing weather details", e);
        }

        return map;
    }
}

