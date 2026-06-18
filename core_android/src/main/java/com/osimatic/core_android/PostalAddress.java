package com.osimatic.core_android;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Utility class providing helper methods for postal address geocoding, validation, and formatting.
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see android.location.Geocoder
 * @see android.location.Address
 * @see <a href="https://developer.android.com/reference/android/location/Geocoder">Geocoder — Android Developers</a>
 */
public class PostalAddress {

	private PostalAddress() {}

	// =============================================================================================
	// Geocoding
	// =============================================================================================

	/**
	 * Asynchronously returns the best-matching {@link Address} for the given {@link Location}.
	 *
	 * <p>On API 33+, uses the asynchronous {@link Geocoder#getFromLocation(double, double, int, Geocoder.GeocodeListener)}.
	 * On older APIs, runs the synchronous call on a background thread.
	 * The callback is invoked with the result or {@code null} if none is found.
	 *
	 * @param context  the application context; must not be {@code null}
	 * @param locale   the locale used to localize the returned address; must not be {@code null}
	 * @param location the location to reverse-geocode; must not be {@code null}
	 * @param callback called with the best-matching {@link Address}, or {@code null}
	 */
	public static void getAddressFromLocation(Context context, Locale locale, Location location, Consumer<Address> callback) {
		getAddressFromCoordinates(context, location.getLatitude(), location.getLongitude(), locale, callback);
	}

	/**
	 * Asynchronously returns the best-matching {@link Address} for the given coordinates.
	 *
	 * <p>On API 33+, uses the asynchronous {@link Geocoder#getFromLocation(double, double, int, Geocoder.GeocodeListener)}.
	 * On older APIs, runs the synchronous call on a background thread.
	 * The callback is invoked with the result or {@code null} if none is found.
	 *
	 * @param context   the application context; must not be {@code null}
	 * @param latitude  the latitude of the point to reverse-geocode
	 * @param longitude the longitude of the point to reverse-geocode
	 * @param locale    the locale used to localize the returned address; must not be {@code null}
	 * @param callback  called with the best-matching {@link Address}, or {@code null}
	 */
	public static void getAddressFromCoordinates(Context context, double latitude, double longitude, Locale locale, Consumer<Address> callback) {
		Geocoder geocoder = new Geocoder(context, locale);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			geocoder.getFromLocation(latitude, longitude, 1, addresses ->
				callback.accept(addresses != null && !addresses.isEmpty() ? addresses.get(0) : null)
			);
		} else {
			new Thread(() -> {
				try {
					//noinspection deprecation
					List<Address> results = geocoder.getFromLocation(latitude, longitude, 1);
					callback.accept(results != null && !results.isEmpty() ? results.get(0) : null);
				} catch (IOException e) {
					Log.e("PostalAddress", "Geocoder failed", e);
					callback.accept(null);
				}
			}).start();
		}
	}

	// =============================================================================================
	// Validation
	// =============================================================================================

	/**
	 * Returns {@code true} if all address fields are blank (null or empty).
	 *
	 * @param street            the street line; may be {@code null}
	 * @param additionalAddress the additional address line; may be {@code null}
	 * @param zipCode           the postal/zip code; may be {@code null}
	 * @param city              the city name; may be {@code null}
	 * @param countryIsoCode    the ISO 3166-1 alpha-2 country code; may be {@code null}
	 * @return {@code true} if all fields are null or empty
	 * @see #isEmpty(String, String, String, String, String, boolean)
	 */
	public static boolean isEmpty(String street, String additionalAddress, String zipCode, String city, String countryIsoCode) {
		return isEmpty(street, additionalAddress, zipCode, city, countryIsoCode, false);
	}

	/**
	 * Returns {@code true} if all relevant address fields are blank (null or empty).
	 *
	 * @param street            the street line; may be {@code null}
	 * @param additionalAddress the additional address line; may be {@code null}
	 * @param zipCode           the postal/zip code; may be {@code null}
	 * @param city              the city name; may be {@code null}
	 * @param countryIsoCode    the ISO 3166-1 alpha-2 country code; may be {@code null}
	 * @param ignoreCountry     {@code true} to exclude the country field from the check
	 * @return {@code true} if all considered fields are null or empty
	 */
	public static boolean isEmpty(String street, String additionalAddress, String zipCode, String city, String countryIsoCode, boolean ignoreCountry) {
		return isBlank(street)
				&& isBlank(additionalAddress)
				&& isBlank(zipCode)
				&& isBlank(city)
				&& (ignoreCountry || isBlank(countryIsoCode));
	}

	/**
	 * Returns {@code true} if the minimum required address fields (street, zip code, city, and country) are all non-blank.
	 *
	 * @param street         the street line; may be {@code null}
	 * @param additionalAddress the additional address line; may be {@code null} (not required)
	 * @param zipCode        the postal/zip code; may be {@code null}
	 * @param city           the city name; may be {@code null}
	 * @param countryIsoCode the ISO 3166-1 alpha-2 country code; may be {@code null}
	 * @return {@code true} if street, zip code, city, and country are all non-blank
	 */
	public static boolean isComplete(String street, String additionalAddress, String zipCode, String city, String countryIsoCode) {
		return !isBlank(street)
				&& !isBlank(zipCode)
				&& !isBlank(city)
				&& !isBlank(countryIsoCode);
	}

	// =============================================================================================
	// Formatting
	// =============================================================================================

	/**
	 * Formats all address lines from the given {@link Address} into a comma-separated string.
	 *
	 * @param address the address to format; must not be {@code null}
	 * @return a comma-separated string of address lines
	 * @see #formatAddressForDisplay(Address, String)
	 */
	public static String formatAddressForDisplay(Address address) {
		return formatAddressForDisplay(address, ", ");
	}

	/**
	 * Formats all address lines from the given {@link Address} into a string joined by the given separator.
	 *
	 * @param address   the address to format; must not be {@code null}
	 * @param separator the separator to insert between address lines; must not be {@code null}
	 * @return a string of address lines joined by {@code separator}
	 */
	public static String formatAddressForDisplay(Address address, String separator) {
		List<String> addressLines = new ArrayList<>();
		for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
			addressLines.add(address.getAddressLine(i));
		}
		return String.join(separator, addressLines);
	}

	/**
	 * Formats the given address fields into a multi-line display string, with each non-empty field on its own line and the country in uppercase.
	 *
	 * <p>Equivalent to calling {@link #formatAddressForDisplay(String, String, String, String, String, boolean, String)} with {@code upperCase=true} and {@code separator="\n"}.
	 *
	 * @param street            the street line; may be {@code null}
	 * @param additionalAddress the additional address line; may be {@code null}
	 * @param zipCode           the postal/zip code; may be {@code null}
	 * @param city              the city name; may be {@code null}
	 * @param countryIsoCode    the ISO 3166-1 alpha-2 country code; may be {@code null}
	 * @return a formatted multi-line address string, or {@code ""} if all fields are blank
	 */
	public static String formatAddressForDisplay(String street, String additionalAddress, String zipCode, String city, String countryIsoCode) {
		return formatAddressForDisplay(street, additionalAddress, zipCode, city, countryIsoCode, true, null);
	}

	/**
	 * Formats the given address fields into a display string.
	 *
	 * <p>Fields that are null or blank are omitted. The zip code and city are placed on the same line, separated by a space. The country is resolved from its ISO 3166-1 alpha-2 code via {@link Country#getDisplayName(String)}.
	 *
	 * @param street            the street line; may be {@code null}
	 * @param additionalAddress the additional address line; may be {@code null}
	 * @param zipCode           the postal/zip code; may be {@code null}
	 * @param city              the city name; may be {@code null}
	 * @param countryIsoCode    the ISO 3166-1 alpha-2 country code; may be {@code null}
	 * @param upperCase         {@code true} to convert all lines to upper case
	 * @param separator         the separator to insert between lines; defaults to {@code "\n"} if {@code null}
	 * @return a formatted address string, or {@code ""} if all fields are blank
	 * @see Country#getDisplayName(String)
	 */
	public static String formatAddressForDisplay(String street, String additionalAddress, String zipCode, String city, String countryIsoCode, boolean upperCase, String separator) {
		String sep = separator != null ? separator : "\n";
		StringBuilder sb = new StringBuilder();

		// street
		if (!isBlank(street)) {
			sb.append(upperCase ? street.toUpperCase(Locale.getDefault()) : street).append(sep);
		}

		// additional address line
		if (!isBlank(additionalAddress)) {
			sb.append(upperCase ? additionalAddress.toUpperCase(Locale.getDefault()) : additionalAddress).append(sep);
		}

		// zip code and city on the same line
		if (!isBlank(zipCode) || !isBlank(city)) {
			String z = isBlank(zipCode) ? "" : (upperCase ? zipCode.toUpperCase(Locale.getDefault()) : zipCode);
			String c = isBlank(city)    ? "" : (upperCase ? city.toUpperCase(Locale.getDefault())    : city);
			sb.append(z).append(isBlank(z) || isBlank(c) ? "" : " ").append(c).append(sep);
		}

		// country
		if (!isBlank(countryIsoCode)) {
			String countryName = Country.getDisplayName(countryIsoCode);
			sb.append(upperCase ? countryName.toUpperCase(Locale.getDefault()) : countryName).append(sep);
		}

		if (sb.length() == 0) {
			return "";
		}

		return sb.substring(0, sb.length() - sep.length());
	}

	/**
	 * Formats the given address fields into a single-line string suitable for use with a geocoding API.
	 *
	 * <pre>
	 * formatForGeocode("10 Downing St", null, "SW1A 2AA", "London", "GB")
	 *     = "10 Downing St, SW1A 2AA London, GB"
	 * </pre>
	 *
	 * @param street            the street line; may be {@code null}
	 * @param additionalAddress the additional address line; may be {@code null}
	 * @param zipCode           the postal/zip code; may be {@code null}
	 * @param city              the city name; may be {@code null}
	 * @param countryIsoCode    the ISO 3166-1 alpha-2 country code; may be {@code null}
	 * @return a single-line address string with components separated by {@code ", "}
	 */
	public static String formatForGeocode(String street, String additionalAddress, String zipCode, String city, String countryIsoCode) {
		List<String> parts = new ArrayList<>();
		if (!isBlank(street))            parts.add(street);
		if (!isBlank(additionalAddress)) parts.add(additionalAddress);
		String zipCity = (isBlank(zipCode) ? "" : zipCode) + ((!isBlank(zipCode) && !isBlank(city)) ? " " : "") + (isBlank(city) ? "" : city);
		if (!zipCity.isEmpty())          parts.add(zipCity);
		if (!isBlank(countryIsoCode))    parts.add(countryIsoCode);
		return String.join(", ", parts);
	}

	// =============================================================================================
	// Private helpers
	// =============================================================================================

	/**
	 * Returns {@code true} if the given string is {@code null} or empty.
	 *
	 * @param s the string to test
	 * @return {@code true} if {@code s} is {@code null} or has zero length
	 */
	private static boolean isBlank(String s) {
		return s == null || s.isEmpty();
	}
}