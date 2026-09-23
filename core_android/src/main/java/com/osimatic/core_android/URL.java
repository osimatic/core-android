package com.osimatic.core_android;

import android.net.Uri;
import android.os.Build;
import android.util.Patterns;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utility class providing helper methods for URL building, parsing, encoding, and validation.
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see android.net.Uri
 * @see <a href="https://developer.android.com/reference/android/net/Uri">android.net.Uri — Android Docs</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc3986">RFC 3986 — Uniform Resource Identifier (URI): Generic Syntax</a>
 */
public class URL {

	private URL() {}

	// =============================================================================================
	// Query string
	// =============================================================================================

	/**
	 * Builds a URL-encoded query string fragment from the given map of parameters.
	 *
	 * <p>Each key and value is percent-encoded using UTF-8. The returned string starts with {@code &} and is intended to be concatenated to an existing query string. If the map is empty, an empty string is returned.
	 *
	 * <pre>
	 * buildQueryString({"q": "hello world"})  = "&amp;q=hello+world"
	 * buildQueryString({})                    = ""
	 * </pre>
	 *
	 * @param params a map of query parameter names to values; must not be {@code null}; values may be {@code null} (treated as empty strings)
	 * @return a URL-encoded query string prefixed with {@code &}, or an empty string if the map is empty
	 * @see #buildUrl(String, Map)
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc3986#section-3.4">RFC 3986 §3.4 — Query</a>
	 */
	public static String buildQueryString(Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			String value = entry.getValue() != null ? entry.getValue() : "";
			sb.append("&")
					.append(encode(entry.getKey()))
					.append("=")
					.append(encode(value));
		}
		return sb.toString();
	}

	/**
	 * Appends a single query parameter to the given URL.
	 *
	 * <pre>
	 * appendQueryParam("https://example.com", "q", "hello")  = "https://example.com?q=hello"
	 * appendQueryParam("https://example.com?a=1", "b", "2")  = "https://example.com?a=1&amp;b=2"
	 * </pre>
	 *
	 * @param url   the base URL; must not be {@code null}
	 * @param key   the query parameter name; must not be {@code null}
	 * @param value the query parameter value; must not be {@code null}
	 * @return the URL with the query parameter appended
	 * @see #buildUrl(String, Map)
	 */
	public static String appendQueryParam(String url, String key, String value) {
		return Uri.parse(url).buildUpon().appendQueryParameter(key, value).build().toString();
	}

	/**
	 * Builds a complete URL by appending the given query parameters to the base URL.
	 *
	 * <pre>
	 * buildUrl("https://example.com", {"q": "hello", "page": "1"})
	 *     = "https://example.com?q=hello&amp;page=1"
	 * </pre>
	 *
	 * @param baseUrl the base URL; must not be {@code null}
	 * @param params  a map of query parameter names to values; must not be {@code null}
	 * @return the full URL with all query parameters appended
	 * @see #buildQueryString(Map)
	 * @see #appendQueryParam(String, String, String)
	 */
	public static String buildUrl(String baseUrl, Map<String, String> params) {
		Uri.Builder builder = Uri.parse(baseUrl).buildUpon();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			builder.appendQueryParameter(entry.getKey(), entry.getValue());
		}
		return builder.build().toString();
	}

	// =============================================================================================
	// Parsing
	// =============================================================================================

	/**
	 * Extracts the value of the given query parameter from the given URL.
	 *
	 * <pre>
	 * getQueryParam("https://example.com?q=hello&amp;page=2", "q")     = "hello"
	 * getQueryParam("https://example.com?q=hello&amp;page=2", "page")  = "2"
	 * getQueryParam("https://example.com", "q")                        = null
	 * </pre>
	 *
	 * @param url the URL to extract the parameter from; must not be {@code null}
	 * @param key the name of the query parameter; must not be {@code null}
	 * @return the decoded value of the query parameter, or {@code null} if not present
	 * @see android.net.Uri#getQueryParameter(String)
	 */
	public static String getQueryParam(String url, String key) {
		return Uri.parse(url).getQueryParameter(key);
	}

	/**
	 * Returns the base URL (scheme + authority + path) stripped of its query string and fragment.
	 *
	 * <pre>
	 * getBaseUrl("https://example.com/path?q=1#section")  = "https://example.com/path"
	 * getBaseUrl("https://example.com")                   = "https://example.com"
	 * </pre>
	 *
	 * @param url the URL to strip; must not be {@code null}
	 * @return the URL without query string or fragment
	 */
	public static String getBaseUrl(String url) {
		return Uri.parse(url).buildUpon().clearQuery().fragment(null).build().toString();
	}

	/**
	 * Returns the file extension (without leading dot) of the last path segment of the given URL, ignoring any query string or fragment.
	 *
	 * <pre>
	 * getExtension("https://example.com/dir/file.pdf?X-Amz-Signature=abc")  = "pdf"
	 * getExtension("https://example.com/dir/file.pdf#section")              = "pdf"
	 * getExtension("https://example.com/dir/file")                          = ""
	 * </pre>
	 *
	 * @param url the URL to inspect; must not be {@code null}
	 * @return the file extension without leading dot, an empty string if none, or {@code null} if the path cannot be determined
	 * @see File#getExtension(String)
	 */
	public static String getExtension(String url) {
		return File.getExtension(Uri.parse(url).getPath());
	}

	// =============================================================================================
	// Encoding / decoding
	// =============================================================================================

	/**
	 * Percent-encodes the given string using UTF-8 for safe inclusion in a URL query string.
	 *
	 * <p>On API 33+ ({@link Build.VERSION_CODES#TIRAMISU}), uses {@link java.net.URLEncoder} which encodes spaces as {@code +}. On lower API levels, falls back to {@link Uri#encode(String)} which encodes spaces as {@code %20}.
	 *
	 * <pre>
	 * encode("café")        = "caf%C3%A9"
	 * encode("hello world") = "hello+world"   // API 33+
	 * encode("hello world") = "hello%20world" // API &lt; 33
	 * </pre>
	 *
	 * @param value the string to encode; must not be {@code null}
	 * @return the URL-encoded string
	 * @see #decode(String)
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc3986#section-2.1">RFC 3986 §2.1 — Percent-Encoding</a>
	 */
	public static String encode(String value) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return URLEncoder.encode(value, StandardCharsets.UTF_8);
		}
		return Uri.encode(value);
	}

	/**
	 * Decodes a percent-encoded URL string using UTF-8.
	 *
	 * <p>On API 33+ ({@link Build.VERSION_CODES#TIRAMISU}), uses {@link java.net.URLDecoder} which also converts {@code +} to a space. On lower API levels, falls back to {@link Uri#decode(String)} which only decodes percent-encoded sequences (e.g. {@code +} is left as-is).
	 *
	 * <pre>
	 * decode("caf%C3%A9")    = "café"
	 * decode("hello%20world") = "hello world"
	 * </pre>
	 *
	 * @param value the string to decode; must not be {@code null}
	 * @return the decoded string
	 * @see #encode(String)
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc3986#section-2.1">RFC 3986 §2.1 — Percent-Encoding</a>
	 */
	public static String decode(String value) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return URLDecoder.decode(value, StandardCharsets.UTF_8);
		}
		return Uri.decode(value);
	}

	// =============================================================================================
	// Validation
	// =============================================================================================

	/**
	 * Returns {@code true} if the given string is a valid HTTP or HTTPS URL.
	 *
	 * <pre>
	 * isValid("https://example.com")   = true
	 * isValid("http://example.com/p")  = true
	 * isValid("ftp://example.com")     = false
	 * isValid("not a url")             = false
	 * isValid(null)                    = false
	 * </pre>
	 *
	 * @param url the string to validate; may be {@code null}
	 * @return {@code true} if {@code url} is a well-formed HTTP or HTTPS URL
	 * @see android.util.Patterns#WEB_URL
	 */
	public static boolean isValid(String url) {
		if (url == null) return false;
		return Patterns.WEB_URL.matcher(url).matches();
	}
}