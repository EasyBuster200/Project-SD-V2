package sd2526.trab.impl.zoho;

import java.util.HashSet;
import java.util.Set;

import sd2526.trab.api.Message;

final class MessageBodyCodec {

	static final String SEPARATOR = "------SD2526------";

	static String encode(Message msg) {
		StringBuilder sb = new StringBuilder();
		sb.append(msg.getContents() == null ? "" : msg.getContents());
		sb.append("\n").append(SEPARATOR).append("\n");
		sb.append("id=").append(msg.getId()).append("\n");
		sb.append("sender=").append(msg.getSender()).append("\n");
		sb.append("destinations=").append(String.join(",", msg.getDestination())).append("\n");
		sb.append("creationTime=").append(msg.getCreationTime());
		return sb.toString();
	}

	static Message decode(String body, String subject) {
		if (body == null)
			return null;

		int sep = body.indexOf(SEPARATOR);
		if (sep < 0)
			return null;

		String contents = body.substring(0, sep);

		if (contents.endsWith("\n"))
			contents = contents.substring(0, contents.length() - 1);

		String meta = body.substring(sep + SEPARATOR.length());

		String id = null;
		String sender = null;
		Set<String> destinations = new HashSet<>();
		long creationTime = 0L;

		for (String line : meta.split("\n")) {
			line = line.trim();
			if (line.isEmpty())
				continue;
			int eq = line.indexOf('=');
			if (eq < 0)
				continue;
			String key = line.substring(0, eq);
			String value = line.substring(eq + 1);
			switch (key) {
				case "id":
					id = value;
					break;
				case "sender":
					sender = value;
					break;
				case "destinations":
					if (!value.isEmpty()) {
						for (String d : value.split(","))
							destinations.add(d);
					}
					break;
				case "creationTime":
					try {
						creationTime = Long.parseLong(value);
					} catch (NumberFormatException ignored) {}
					break;
			}
		}

		Message m = new Message(id, sender, destinations, subject, contents);
		m.setCreationTime(creationTime);
		return m;
	}

	private MessageBodyCodec() {}
}