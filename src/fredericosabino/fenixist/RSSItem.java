package fredericosabino.fenixist;

import java.io.Serializable;

public class RSSItem implements Serializable {
	private static final long serialVersionUID = 53613094403091403L;
	private String _title;
	private String _link;
	private String _description;
	private String _pubDate;
	private String _lastBuildDate;
	private String _guid;
	
	public RSSItem(String _title, String _link, String _description,
			String _pubDate, String _lastBuildDate, String _guid) {
		super();
		this._title = _title;
		this._link = _link;
		this._description = _description;
		this._pubDate = _pubDate;
		this._lastBuildDate = _lastBuildDate;
		this._guid = _guid;
	}
	
	public RSSItem() {}

	public String get_title() {
		return _title;
	}

	public String get_link() {
		return _link;
	}

	public String get_description() {
		return _description;
	}

	public String get_pubDate() {
		return _pubDate;
	}

	public String get_lastBuildDate() {
		return _lastBuildDate;
	}

	public String get_guid() {
		return _guid;
	}
}
