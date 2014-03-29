package fredericosabino.fenixist.fenixdata;

import java.io.Serializable;

public class AboutInfo implements Serializable {
	private static final long serialVersionUID = -4337746835175662358L;
	private String institutionUrl;
	
	private PublicFeed[] rssFeeds;
	
	public class PublicFeed {
		private String description;
		private String url;
		
		public String getDescription() {
			return description;
		}
		public String getUrl() {
			return url;
		}
		
		public String toString() {
			return description + " " + url;
		}
	}

	public PublicFeed[] getRssFeeds() {
		return rssFeeds;
	}
	
	public String getInstitutionUrl() {
		return institutionUrl;
	}
}
