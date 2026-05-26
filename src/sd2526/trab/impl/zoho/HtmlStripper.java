package sd2526.trab.impl.zoho;

import java.util.regex.Pattern;

/**
 * Helper class that recovers the origianl text content from the HTML wrapped
 * body Zoho returns for {@code /content} requests.
 */
final class HtmlStripper {

	private static final Pattern TAG = Pattern.compile("<[^>]+>");

	static String strip(String html) {
		if (html == null)
			return null;

		String s = html;

		s = s.replace("\r<br>", "\n");
		s = s.replace("\r<br/>", "\n");
		s = s.replace("\r<br />", "\n");
		s = s.replace("<br>", "\n");
		s = s.replace("<br/>", "\n");
		s = s.replace("<br />", "\n");

		s = s.replace("\r", "");

		s = TAG.matcher(s).replaceAll("");

		s = s.replace("&nbsp;", " ");
		s = s.replace("&quot;", "\"");
		s = s.replace("&#39;", "'");
		s = s.replace("&apos;", "'");
		s = s.replace("&lt;", "<");
		s = s.replace("&gt;", ">");
		s = s.replace("&amp;", "&");

		s = s.replaceAll("\\s+$", "");

		return s;
	}

	private HtmlStripper() {
	}
}