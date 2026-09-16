package com.osimatic.core_android;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Utility class providing helper methods for working with country codes and country-related data.
 *
 * <p>Country codes follow the ISO 3166-1 alpha-2 standard (two uppercase letters, e.g. {@code "FR"}, {@code "US"}).
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see Locale
 * @see <a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2 — Wikipedia</a>
 */
public class Country {

	private Country() {}

	// =============================================================================================
	// Display
	// =============================================================================================

	/**
	 * Returns the display name of the country identified by the given ISO 3166-1 alpha-2 code, localized using the device's default locale.
	 *
	 * @param countryCode a two-letter ISO 3166-1 alpha-2 country code (e.g. {@code "FR"}, {@code "US"}); must not be {@code null}
	 * @return the localized display name of the country, or an empty string if the code is not recognized
	 * @see Locale#getDisplayCountry()
	 * @see <a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2 — Wikipedia</a>
	 */
	public static String getDisplayName(String countryCode) {
		return getDisplayName(countryCode, Locale.getDefault());
	}

	/**
	 * Returns the display name of the country identified by the given ISO 3166-1 alpha-2 code, localized in the given locale.
	 *
	 * @param countryCode   a two-letter ISO 3166-1 alpha-2 country code (e.g. {@code "FR"}, {@code "US"}); must not be {@code null}
	 * @param displayLocale the locale to use for the display name; must not be {@code null}
	 * @return the localized display name of the country, or an empty string if the code is not recognized
	 * @see Locale#getDisplayCountry(Locale)
	 * @see <a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2 — Wikipedia</a>
	 */
	public static String getDisplayName(String countryCode, Locale displayLocale) {
		return toLocale(countryCode).getDisplayCountry(displayLocale);
	}

	/** @deprecated Use {@link #getDisplayName(String)} instead. */
	@Deprecated
	public static String getCountryNameFromCountryCode(String countryCode) {
		return getDisplayName(countryCode);
	}

	/**
	 * Returns the Unicode flag emoji for the country identified by the given ISO 3166-1 alpha-2 code.
	 *
	 * <p>Flag emojis are composed of two Unicode Regional Indicator Symbol Letters corresponding to the country code letters. For example, {@code "FR"} produces {@code "🇫🇷"}.
	 *
	 * @param countryCode a two-letter ISO 3166-1 alpha-2 country code (e.g. {@code "FR"}, {@code "US"}); must not be {@code null}
	 * @return the flag emoji string for the country
	 * @throws IllegalArgumentException if {@code countryCode} does not have exactly 2 characters
	 * @see <a href="https://en.wikipedia.org/wiki/Regional_indicator_symbol">Regional Indicator Symbol — Wikipedia</a>
	 */
	public static String getFlagEmoji(String countryCode) {
		if (countryCode == null || countryCode.length() != 2) {
			throw new IllegalArgumentException("Country code must be exactly 2 characters");
		}
		String upper = countryCode.toUpperCase(Locale.US);
		int first  = Character.codePointAt(upper, 0) - 'A' + 0x1F1E6;
		int second = Character.codePointAt(upper, 1) - 'A' + 0x1F1E6;
		return new String(Character.toChars(first)) + new String(Character.toChars(second));
	}

	// =============================================================================================
	// Validation
	// =============================================================================================

	/**
	 * Returns {@code true} if the given string is a valid ISO 3166-1 alpha-2 country code.
	 *
	 * <p>Validation is performed against the list returned by {@link Locale#getISOCountries()}.
	 *
	 * @param countryCode the string to validate; may be {@code null}
	 * @return {@code true} if {@code countryCode} is a recognized ISO 3166-1 alpha-2 code, {@code false} otherwise
	 * @see <a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2 — Wikipedia</a>
	 */
	public static boolean isValidCountryCode(String countryCode) {
		if (countryCode == null || countryCode.length() != 2) {
			return false;
		}
		for (String code : Locale.getISOCountries()) {
			if (code.equalsIgnoreCase(countryCode)) {
				return true;
			}
		}
		return false;
	}

	// =============================================================================================
	// Utilities
	// =============================================================================================

	/**
	 * Returns a list of all ISO 3166-1 alpha-2 country codes recognized by the JVM.
	 *
	 * @return a list of two-letter ISO country codes
	 * @see Locale#getISOCountries()
	 * @see <a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2 — Wikipedia</a>
	 */
	public static List<String> getAvailableCountryCodes() {
		return Arrays.asList(Locale.getISOCountries());
	}

	/**
	 * Returns a {@link Locale} for the given ISO 3166-1 alpha-2 country code, with no language specified.
	 *
	 * @param countryCode a two-letter ISO 3166-1 alpha-2 country code (e.g. {@code "FR"}, {@code "US"}); must not be {@code null}
	 * @return a {@link Locale} with the given country code and no language
	 * @see Locale
	 */
	public static Locale toLocale(String countryCode) {
		return new Locale.Builder().setRegion(countryCode).build();
	}
}