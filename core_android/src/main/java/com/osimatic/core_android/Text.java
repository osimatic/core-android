package com.osimatic.core_android;

import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.text.Normalizer;

/**
 * Utility class providing helper methods for text and string manipulation.
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see android.text.SpannableString
 * @see java.text.Normalizer
 */
public class Text {

	private Text() {}

	// =============================================================================================
	// TextView utilities
	// =============================================================================================

	/**
	 * Applies the visual link style to the entire text of the given {@link TextView}, without attaching an actual URL.
	 *
	 * <p>This is useful to display a label (e.g. an address) with the standard hyperlink appearance while keeping the click behavior managed externally via an {@code OnClickListener}.
	 *
	 * @param tv the {@link TextView} whose text should be styled as a link; must not be {@code null}
	 * @see URLSpan
	 * @see SpannableString
	 */
	public static void setLinkStyle(TextView tv) {
		final CharSequence text = tv.getText();
		final SpannableString spannableString = new SpannableString(text);
		spannableString.setSpan(new URLSpan(""), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		tv.setText(spannableString, TextView.BufferType.SPANNABLE);
	}

	// =============================================================================================
	// Keyboard utilities
	// =============================================================================================

	/**
	 * Hides the on-screen keyboard (IME) currently associated with the given view, if any.
	 *
	 * <pre>
	 * Text.hideKeyboard(activity.getCurrentFocus());
	 * </pre>
	 *
	 * @param view the currently focused view (e.g. from {@code Activity.getCurrentFocus()}); may be {@code null}
	 * @see WindowInsetsControllerCompat#hide(int)
	 * @see WindowInsetsCompat.Type#ime()
	 */
	public static void hideKeyboard(View view) {
		if (view == null) {
			return;
		}
		WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(view);
		if (controller != null) {
			controller.hide(WindowInsetsCompat.Type.ime());
		}
	}

	// =============================================================================================
	// String checks
	// =============================================================================================

	/**
	 * Returns {@code true} if the given string is {@code null} or empty.
	 *
	 * @param str the string to test; may be {@code null}
	 * @return {@code true} if {@code str} is {@code null} or has zero length
	 */
	public static boolean isNullOrEmpty(String str) {
		return str == null || str.isEmpty();
	}

	/**
	 * Returns {@code true} if the given string is {@code null}, empty, or contains only whitespace characters.
	 *
	 * @param str the string to test; may be {@code null}
	 * @return {@code true} if {@code str} is {@code null} or blank
	 */
	public static boolean isNullOrBlank(String str) {
		return str == null || str.trim().isEmpty();
	}

	// =============================================================================================
	// String manipulation
	// =============================================================================================

	/**
	 * Capitalizes the first character of the given string using Unicode title case.
	 *
	 * <p>Unlike {@link String#toUpperCase()}, this method correctly handles Unicode supplementary characters (code points above U+FFFF).
	 *
	 * <pre>
	 * capitalize(null)     = null
	 * capitalize("")       = ""
	 * capitalize("hello")  = "Hello"
	 * capitalize("HELLO")  = "HELLO"
	 * </pre>
	 *
	 * @param str the string to capitalize; may be {@code null}
	 * @return the string with its first character in title case, or the original string if already capitalized, {@code null}, or empty
	 * @see Character#toTitleCase(int)
	 */
	public static String capitalize(final String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return str;
		}
		final int firstCodepoint = str.codePointAt(0);
		final int newCodePoint = Character.toTitleCase(firstCodepoint);
		if (firstCodepoint == newCodePoint) {
			// already capitalized
			return str;
		}
		final int[] newCodePoints = new int[strLen]; // cannot be longer than the char array
		int outOffset = 0;
		newCodePoints[outOffset++] = newCodePoint; // copy the first codepoint
		for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen; ) {
			final int codepoint = str.codePointAt(inOffset);
			newCodePoints[outOffset++] = codepoint; // copy the remaining ones
			inOffset += Character.charCount(codepoint);
		}
		return new String(newCodePoints, 0, outOffset);
	}

	/**
	 * Truncates the given string to the specified maximum length, appending {@code "…"} if truncation occurred.
	 *
	 * <pre>
	 * truncate("Hello world", 5)   = "Hello…"
	 * truncate("Hi", 5)            = "Hi"
	 * truncate(null, 5)            = null
	 * </pre>
	 *
	 * @param str       the string to truncate; may be {@code null}
	 * @param maxLength the maximum length of the returned string, not counting the ellipsis; must be ≥ 0
	 * @return the truncated string with {@code "…"} appended if shortened, or the original string if short enough
	 * @see #truncate(String, int, String)
	 */
	public static String truncate(String str, int maxLength) {
		return truncate(str, maxLength, "…");
	}

	/**
	 * Truncates the given string to the specified maximum length, appending a custom suffix if truncation occurred.
	 *
	 * <pre>
	 * truncate("Hello world", 5, "...")  = "Hello..."
	 * truncate("Hi", 5, "...")           = "Hi"
	 * truncate(null, 5, "...")           = null
	 * </pre>
	 *
	 * @param str       the string to truncate; may be {@code null}
	 * @param maxLength the maximum number of characters to keep from the original string; must be ≥ 0
	 * @param suffix    the suffix to append when truncation occurs; must not be {@code null}
	 * @return the truncated string with suffix appended if shortened, or the original string if short enough
	 */
	public static String truncate(String str, int maxLength, String suffix) {
		if (str == null) return null;
		if (str.length() <= maxLength) return str;
		return str.substring(0, maxLength) + suffix;
	}

	/**
	 * Removes diacritical marks (accents) from the given string using Unicode NFD normalization.
	 *
	 * <pre>
	 * removeAccents("éàü")  = "eau"
	 * removeAccents("café") = "cafe"
	 * removeAccents(null)   = null
	 * </pre>
	 *
	 * @param str the string to process; may be {@code null}
	 * @return the string with all combining diacritical marks removed, or {@code null} if input is {@code null}
	 * @see Normalizer#normalize(CharSequence, Normalizer.Form)
	 * @see <a href="https://unicode.org/reports/tr15/">Unicode Normalization Forms — Unicode Standard Annex #15</a>
	 */
	public static String removeAccents(String str) {
		if (str == null) return null;
		return Normalizer.normalize(str, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	/**
	 * Returns the initials of the given full name, one uppercase letter per word.
	 *
	 * <pre>
	 * initials("Jean Dupont")        = "JD"
	 * initials("Marie-Claire Duval") = "MD"
	 * initials("alice")              = "A"
	 * initials(null)                 = ""
	 * </pre>
	 *
	 * @param fullName the full name to extract initials from; may be {@code null}
	 * @return a string of uppercase initials, one per whitespace-separated word; never {@code null}
	 */
	public static String initials(String fullName) {
		if (isNullOrBlank(fullName)) return "";
		StringBuilder sb = new StringBuilder();
		for (String word : fullName.trim().split("\\s+")) {
			if (!word.isEmpty()) {
				sb.appendCodePoint(Character.toUpperCase(word.codePointAt(0)));
			}
		}
		return sb.toString();
	}

	// =============================================================================================
	// HTML utilities
	// =============================================================================================

	/**
	 * Removes all HTML tags from the given string.
	 *
	 * <pre>
	 * stripHtml("&lt;b&gt;Hello&lt;/b&gt; &lt;i&gt;world&lt;/i&gt;")  = "Hello world"
	 * stripHtml("No tags here")                                             = "No tags here"
	 * stripHtml(null)                                                       = null
	 * </pre>
	 *
	 * @param html the HTML string to strip; may be {@code null}
	 * @return the string with all HTML tags removed, or {@code null} if input is {@code null}
	 */
	public static String stripHtml(String html) {
		if (html == null) return null;
		return html.replaceAll("<[^>]+>", "");
	}

	/**
	 * Converts an HTML string to a {@link Spanned}, using the legacy HTML parser
	 * (same rendering behavior as the deprecated single-argument {@code Html.fromHtml(String)}).
	 *
	 * @param html the HTML string to convert; may be {@code null}
	 * @return the parsed {@link Spanned}, or {@code null} if {@code html} is {@code null}
	 * @see Html#fromHtml(String, int)
	 */
	public static Spanned fromHtml(String html) {
		if (html == null) {
			return null;
		}
		return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
	}

	/**
	 * Returns a string consisting of the given string repeated {@code count} times.
	 *
	 * <pre>
	 * repeat("ab", 3)   = "ababab"
	 * repeat("ab", 0)   = ""
	 * repeat(null, 3)   = null
	 * </pre>
	 *
	 * @param str   the string to repeat; may be {@code null}
	 * @param count the number of times to repeat; must be ≥ 0
	 * @return the repeated string, or {@code null} if {@code str} is {@code null}
	 * @throws IllegalArgumentException if {@code count} is negative
	 */
	public static String repeat(String str, int count) {
		if (str == null) return null;
		if (count < 0) throw new IllegalArgumentException("count must be >= 0");
		if (count == 0 || str.isEmpty()) return "";
		StringBuilder sb = new StringBuilder(str.length() * count);
		for (int i = 0; i < count; i++) {
			sb.append(str);
		}
		return sb.toString();
	}
}