package sd2526.trab.impl.utils;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Wrapper around a shared Gson instance for JSON encoding/decoding.
 */
final public class JSON {
	private static final Gson gson = new Gson();

	/** Encodes any object to its JSON representation */
	synchronized public static final String encode(Object obj) {
		return gson.toJson(obj);
	}

	/** Decodes a JSON string into an instance of the given class */
	synchronized public static final <T> T decode(String json, Class<T> classOf) {
		return gson.fromJson(json, classOf);
	}

	/** Decodes a JSON string into a generic type */
	@SuppressWarnings("unchecked")
	synchronized public static <T> T decode(String key, TypeToken<?> typeOf) {
		return (T) gson.fromJson(key, typeOf.getType());
	}

	/** Round-trips any object through JSON to produce a generic map view */
	@SuppressWarnings("unchecked")
	synchronized public static Map<String, Object> toMap(Object x) {
		return decode(encode(x), Map.class);
	}
}
