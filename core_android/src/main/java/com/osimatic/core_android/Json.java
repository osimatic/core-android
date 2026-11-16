package com.osimatic.core_android;

import org.json.JSONException;
import org.json.JSONObject;

public class Json {
	public static String getString(JSONObject json, String key, String defaultValue) throws JSONException {
		return json.has(key) && !json.isNull(key) && !json.getString(key).equals("") ? json.getString(key) : defaultValue;
	}
	public static String getStringOrNull(JSONObject json, String key) throws JSONException {
		return json.has(key) && !json.isNull(key) && !json.getString(key).equals("") ? json.getString(key) : null;
	}

	public static Integer getInt(JSONObject json, String key) throws JSONException {
		return getInt(json, key, 0);
	}
	public static Integer getInt(JSONObject json, String key, Integer defaultValue) throws JSONException {
		return json.has(key) && !json.isNull(key) ? json.getInt(key) : defaultValue;
	}
	public static Integer getIntOrNull(JSONObject json, String key) throws JSONException {
		return json.has(key) && !json.isNull(key) ? json.getInt(key) : null;
	}

	public static Double getDouble(JSONObject json, String key) throws JSONException {
		return getDouble(json, key, 0.);
	}
	public static Double getDouble(JSONObject json, String key, Double defaultValue) throws JSONException {
		return json.has(key) && !json.isNull(key) ? json.getDouble(key) : defaultValue;
	}
	public static Double getDoubleOrNull(JSONObject json, String key) throws JSONException {
		return json.has(key) && !json.isNull(key) ? json.getDouble(key) : null;
	}

	public static boolean getBool(JSONObject json, String key) throws JSONException {
		return getBoolean(json, key, false);
	}
	public static boolean getBool(JSONObject json, String key, boolean defaultValue) throws JSONException {
		return getBoolean(json, key, defaultValue);
	}
	public static boolean getBoolean(JSONObject json, String key) throws JSONException {
		return getBoolean(json, key, false);
	}
	public static boolean getBoolean(JSONObject json, String key, boolean defaultValue) throws JSONException {
		return json.has(key) && !json.isNull(key) ? json.getBoolean(key) : defaultValue;
	}
	
	/** Écrit la valeur uniquement si elle n'est ni null ni vide (la clé reste absente sinon). */
	public static void putIfNotEmpty(JSONObject json, String key, String value) throws JSONException {
		if (null != value && !value.isEmpty()) {
			json.put(key, value);
		}
	}
}
