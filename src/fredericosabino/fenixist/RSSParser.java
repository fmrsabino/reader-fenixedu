package fredericosabino.fenixist;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;


public class RSSParser {
	private XmlPullParser xpp;
	private static final String TAG = "Fenix@IST";
	private static final String ns = null; // don't use namespaces
	
	//RSS tag codes
	public static final String RSS_ITEM = "item";
	public static final String RSS_CHANNEL = "channel";
	public static final String RSS_TITLE = "title";
	public static final String RSS_LINK = "link";
	public static final String RSS_DESCRIPTION = "description";
	public static final String RSS_PUBDATE = "pubDate";
	public static final String RSS_LASTBUILD = "lastBuildDate";
	public static final String RSS_GUID = "guid";
	
	
	
	public RSSParser() throws XmlPullParserException {
		xpp = Xml.newPullParser();
		xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	}
	
	public ArrayList<RSSItem> parse(InputStream stream) throws XmlPullParserException, IOException {
		xpp.setInput(stream, null);
		xpp.nextTag(); //From START_DOCUMENT to the first real tag of the document
		Log.v(TAG, stream.toString());
		ArrayList<RSSItem> list = new ArrayList<RSSItem>();
		
		xpp.require(XmlPullParser.START_TAG, ns, "rss");
		while(xpp.next() != XmlPullParser.END_TAG) {
			if(xpp.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = xpp.getName();
			if(name.equalsIgnoreCase(RSS_CHANNEL)) {
				Log.v("RSSParse", "Channel Found");
				list = readChannel(xpp);
			}
			else {
				skip(xpp);
			}
		}
		stream.close();
		Log.v(TAG, "Parse of RSS finished!");
		return list;
	}
	
	private ArrayList<RSSItem> readChannel(XmlPullParser parser) throws XmlPullParserException, IOException {
		ArrayList<RSSItem> list = new ArrayList<RSSItem>();
		
		parser.require(XmlPullParser.START_TAG, ns, RSS_CHANNEL);
		while(xpp.next() != XmlPullParser.END_TAG) {
			if(xpp.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if(name.equalsIgnoreCase(RSS_ITEM)) {
				Log.v("RSSParse", "Item Found");
				list.add(readItem(parser));
			}
			else {
				skip(parser);
			}
		}
		return list;
	}
	
	private RSSItem readItem(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "item");
		String title = "";
		String link = "";
		String description = "";
		String pubDate = "";
		String lastBuildDate = "";
		String guid = "";
		
		while(parser.next() != XmlPullParser.END_TAG) { //esta END_TAG terá de corresponder ao de item! Os reads+skip consomem os END_TAGs respectivos
			if(parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if(name.equalsIgnoreCase(RSS_TITLE)) {
				Log.v("RSSParse", "Title Found");
				title = readTitle(parser);
			}
			else if(name.equalsIgnoreCase(RSS_LINK)) {
				Log.v("RSSParse", "Link Found");
				link = readLink(parser);
			}
			else if(name.equalsIgnoreCase(RSS_DESCRIPTION)) {
				Log.v("RSSParse", "Description Found");
				description = readDescription(parser);
			}
			else if(name.equalsIgnoreCase(RSS_PUBDATE)) {
				Log.v("RSSParse", "Publication Date Found");
				pubDate = readPubDate(parser);
			}
			else if(name.equalsIgnoreCase(RSS_LASTBUILD)) {
				Log.v("RSSParse", "Last Build Found");
				lastBuildDate = readLastBuild(parser);
			}
			else if(name.equalsIgnoreCase(RSS_GUID)) {
				Log.v("RSSParse", "Guid Found");
				guid = readGuid(parser);
			}
			else {
				skip(parser);
			}
		}
		return new RSSItem(title, link, description, pubDate, lastBuildDate, guid);
	}

	//Ignores everything that we don't want to consume starting with the first tag that is in that condition
	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if(parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		//this algorithm only stops when depth is 0, that means we've consumed all the child tags including the end tag for the starter tag
		int depth = 1;
		while(depth != 0) {
			switch(parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}	
		}
	}

	private String readGuid(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, RSS_GUID);
		String guid = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, RSS_GUID); //should be an END_TAG after reading the text
		return guid;
	}

	private String readLastBuild(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, RSS_LASTBUILD);
		String lastBuild = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, RSS_LASTBUILD); //should be an END_TAG after reading the text
		return lastBuild;
	}

	private String readPubDate(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, RSS_PUBDATE);
		String pubDate = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, RSS_PUBDATE); //should be an END_TAG after reading the text
		return pubDate;
	}

	private String readDescription(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, RSS_DESCRIPTION);
		String description = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, RSS_DESCRIPTION); //should be an END_TAG after reading the text
		return description;
	}

	private String readLink(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, RSS_LINK);
		String link = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, RSS_LINK); //should be an END_TAG after reading the text
		return link;
	}

	private String readTitle(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, RSS_TITLE);
		String title = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, RSS_TITLE); //should be an END_TAG after reading the text
		return title;
	}

	private String readText(XmlPullParser parser) throws XmlPullParserException, IOException {
		String result = "";
		if(parser.next() == XmlPullParser.TEXT) { //goes to the next parsing event if it is Text then copies it to the result
			result = parser.getText();
			parser.nextTag(); //goes to the next parsing event but throws an exception if it isn't a START_TAG or END_TAG
		}
		return result;
	}
}
