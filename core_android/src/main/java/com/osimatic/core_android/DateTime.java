package com.osimatic.core_android;

import android.content.res.Resources;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class providing helper methods for {@link Calendar}-based date and time operations.
 *
 * <p>This class complements {@link Timestamp} (which operates on {@code long} millisecond values) by providing the same operations on {@link Calendar} objects.
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see Timestamp
 * @see DatePeriod
 * @see java.util.Calendar
 */
public class DateTime {

	private DateTime() {}

	// =============================================================================================
	// Construction / factory
	// =============================================================================================

	/**
	 * Returns a new {@link Calendar} set to the current date and time in the default time zone and locale.
	 *
	 * @return a {@link Calendar} representing the current instant
	 * @see Calendar#getInstance()
	 */
	public static Calendar now() {
		return Calendar.getInstance();
	}

	/**
	 * Returns a new {@link Calendar} set to midnight (00:00:00.000) of the given date.
	 *
	 * <p>The {@code month} parameter is 1-based (1 = January, 12 = December).
	 *
	 * @param year  the year (e.g. {@code 2024})
	 * @param month the month, 1-based (1–12)
	 * @param day   the day of month (1–31)
	 * @return a new {@link Calendar} for the given date at midnight
	 */
	public static Calendar make(int year, int month, int day) {
		return new GregorianCalendar(year, month - 1, day);
	}

	/**
	 * Returns a new {@link Calendar} set to the given date and time.
	 *
	 * <p>The {@code month} parameter is 1-based (1 = January, 12 = December).
	 *
	 * @param year   the year (e.g. {@code 2024})
	 * @param month  the month, 1-based (1–12)
	 * @param day    the day of month (1–31)
	 * @param hour   the hour of day (0–23)
	 * @param minute the minute (0–59)
	 * @return a new {@link Calendar} for the given date and time
	 */
	public static Calendar make(int year, int month, int day, int hour, int minute) {
		return new GregorianCalendar(year, month - 1, day, hour, minute);
	}

	/**
	 * Returns a new {@link Calendar} set to the given date and time.
	 *
	 * <p>The {@code month} parameter is 1-based (1 = January, 12 = December).
	 *
	 * @param year   the year (e.g. {@code 2024})
	 * @param month  the month, 1-based (1–12)
	 * @param day    the day of month (1–31)
	 * @param hour   the hour of day (0–23)
	 * @param minute the minute (0–59)
	 * @param second the second (0–59)
	 * @return a new {@link Calendar} for the given date and time
	 */
	public static Calendar make(int year, int month, int day, int hour, int minute, int second) {
		return new GregorianCalendar(year, month - 1, day, hour, minute, second);
	}

	/**
	 * Returns a {@link Calendar} set to midnight (00:00:00.000) of the current local date, expressed in the given time zone.
	 *
	 * <p>The local date (year, month, day) is read from the device's default time zone so that the returned instant always corresponds to "today as the user perceives it", regardless of the target time zone. This avoids the common off-by-one-day issue that occurs in the first hours of the day when UTC is behind the local time zone (e.g. 00:30 UTC+1 → still yesterday in UTC).
	 *
	 * @param targetTimeZone the time zone in which the resulting {@link Calendar} is expressed; must not be {@code null}
	 * @return a new {@link Calendar} at midnight (00:00:00.000) of today's local date in {@code targetTimeZone}
	 * @see #getTodayAtMidnightUtc()
	 */
	public static Calendar getTodayAtMidnight(TimeZone targetTimeZone) {
		Calendar localNow = Calendar.getInstance();
		Calendar cal = Calendar.getInstance(targetTimeZone, Locale.getDefault());
		cal.set(localNow.get(Calendar.YEAR), localNow.get(Calendar.MONTH), localNow.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}

	/**
	 * Returns a {@link Calendar} at midnight (00:00:00.000 UTC) of the current local date.
	 *
	 * <p>Equivalent to {@code getTodayAtMidnight(TimeZone.getTimeZone("UTC"))}.
	 *
	 * @return a new {@link Calendar} at midnight UTC of today's local date
	 * @see #getTodayAtMidnight(TimeZone)
	 */
	public static Calendar getTodayAtMidnightUtc() {
		return getTodayAtMidnight(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Returns a {@link Calendar} set to the first day of the given month at midnight.
	 *
	 * @param year  the year (e.g. {@code 2024})
	 * @param month the month, 1-based (1–12)
	 * @return a {@link Calendar} for the first day of the given month
	 * @see #getLastDayOfMonth(int, int)
	 */
	public static Calendar getFirstDayOfMonth(int year, int month) {
		return make(year, month, 1);
	}

	/**
	 * Returns a {@link Calendar} set to the last day of the given month at midnight.
	 *
	 * @param year  the year (e.g. {@code 2024})
	 * @param month the month, 1-based (1–12)
	 * @return a {@link Calendar} for the last day of the given month
	 * @see #getFirstDayOfMonth(int, int)
	 * @see #getNumberOfDaysOfMonth(int, int)
	 */
	public static Calendar getLastDayOfMonth(int year, int month) {
		return make(year, month, getNumberOfDaysOfMonth(year, month));
	}

	// =============================================================================================
	// Formatting
	// =============================================================================================

	/**
	 * Formats the date part of the given calendar as a short localized date string.
	 *
	 * @param calendar the calendar to format; must not be {@code null}
	 * @param locale   the locale to use for formatting; must not be {@code null}
	 * @param timeZone the time zone ID to use (e.g. {@code "Europe/Paris"}); must not be {@code null}
	 * @return a short localized date string (e.g. {@code "01/01/2024"})
	 * @see Timestamp#formatDateShort(long, Locale, String)
	 */
	public static String formatDateShort(Calendar calendar, Locale locale, String timeZone) {
		return Timestamp.formatDateShort(calendar.getTimeInMillis(), locale, timeZone);
	}

	/**
	 * Formats the time part of the given calendar as a localized time string.
	 *
	 * @param calendar the calendar to format; must not be {@code null}
	 * @param locale   the locale to use for formatting; must not be {@code null}
	 * @param timeZone the time zone ID to use (e.g. {@code "Europe/Paris"}); must not be {@code null}
	 * @return a localized time string (e.g. {@code "14:30"})
	 * @see Timestamp#formatTime(long, Locale, String)
	 */
	public static String formatTime(Calendar calendar, Locale locale, String timeZone) {
		return Timestamp.formatTime(calendar.getTimeInMillis(), locale, timeZone);
	}

	/**
	 * Formats the given calendar as a localized date and time string using the {@code R.string.formatted_date_time} resource pattern.
	 *
	 * @param calendar the calendar to format; must not be {@code null}
	 * @param r        the {@link Resources} used to retrieve the format string; must not be {@code null}
	 * @param locale   the locale to use for formatting; must not be {@code null}
	 * @param timeZone the time zone ID to use (e.g. {@code "Europe/Paris"}); must not be {@code null}
	 * @return a localized date and time string (e.g. {@code "01/01/2024 14:30"})
	 * @see #formatDateShort(Calendar, Locale, String)
	 * @see #formatTime(Calendar, Locale, String)
	 */
	public static String formatDateTime(Calendar calendar, Resources r, Locale locale, String timeZone) {
		return String.format(r.getString(R.string.formatted_date_time),
				formatDateShort(calendar, locale, timeZone),
				formatTime(calendar, locale, timeZone));
	}

	/**
	 * Formats the given calendar as an SQL date string ({@code yyyy-MM-dd}), using the calendar's own time zone.
	 *
	 * <p>Unlike {@code new java.sql.Date(calendar.getTimeInMillis()).toString()}, this does not depend on the JVM/device default time zone: the calendar's own {@link TimeZone} (as set by {@link Calendar#setTimeZone}) is what determines the resulting day.
	 *
	 * @param calendar the calendar to format; must not be {@code null}
	 * @return an SQL date string (e.g. {@code "2024-01-31"})
	 */
	public static String getSqlDate(Calendar calendar) {
		return Timestamp.getSqlDate(calendar.getTimeInMillis(), calendar.getTimeZone().getID());
	}

	/**
	 * Formats the given calendar as an SQL time string ({@code HH:mm:ss}), using the calendar's own time zone.
	 *
	 * <p>Unlike {@code new java.sql.Time(calendar.getTimeInMillis()).toString()}, this does not depend on the JVM/device default time zone: the calendar's own {@link TimeZone} (as set by {@link Calendar#setTimeZone}) is what determines the resulting time.
	 *
	 * @param calendar the calendar to format; must not be {@code null}
	 * @return an SQL time string (e.g. {@code "14:30:00"})
	 */
	public static String getSqlTime(Calendar calendar) {
		return Timestamp.getSqlTime(calendar.getTimeInMillis(), calendar.getTimeZone().getID());
	}

	/**
	 * Formats the given calendar as an SQL datetime string ({@code yyyy-MM-dd HH:mm:ss}).
	 *
	 * @param calendar the calendar to format; must not be {@code null}
	 * @return an SQL datetime string (e.g. {@code "2024-01-31 14:30:00"})
	 */
	public static String getSqlDateTime(Calendar calendar) {
		return getSqlDate(calendar) + " " + getSqlTime(calendar);
	}

	// =============================================================================================
	// Parsing
	// =============================================================================================

	/**
	 * Parses an SQL datetime string ({@code yyyy-MM-dd HH:mm:ss}) in UTC and returns the corresponding {@link Calendar}.
	 *
	 * @param sqlDateTime an SQL datetime string (e.g. {@code "2024-01-31 14:30:00"}); must not be {@code null}
	 * @return the parsed {@link Calendar}, or {@code null} if parsing fails
	 * @see #parse(String, TimeZone)
	 */
	public static Calendar parse(String sqlDateTime) {
		return parse(sqlDateTime, TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Parses an SQL datetime string ({@code yyyy-MM-dd HH:mm:ss}) in the given time zone and returns the corresponding {@link Calendar}.
	 *
	 * @param sqlDateTime an SQL datetime string (e.g. {@code "2024-01-31 14:30:00"}); must not be {@code null}
	 * @param timeZone    the time zone to use for parsing; must not be {@code null}
	 * @return the parsed {@link Calendar}, or {@code null} if parsing fails
	 */
	public static Calendar parse(String sqlDateTime, TimeZone timeZone) {
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			df.setTimeZone(timeZone);
			Calendar calendar = Calendar.getInstance();
			Date date = df.parse(sqlDateTime);
			if (date != null) {
				calendar.setTime(date);
			}
			return calendar;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	// =============================================================================================
	// Calendar fields / helpers
	// =============================================================================================

	/**
	 * Returns {@code true} if the given date components correspond to today's date.
	 *
	 * <p>The {@code month} parameter is 1-based (1 = January, 12 = December).
	 *
	 * @param year  the year to test
	 * @param month the month to test, 1-based (1–12)
	 * @param day   the day of month to test (1–31)
	 * @return {@code true} if the given date is today
	 * @see #isToday(Calendar)
	 */
	public static boolean isToday(int year, int month, int day) {
		Calendar today = Calendar.getInstance(Locale.getDefault());
		return year  == today.get(Calendar.YEAR)
				&& month == today.get(Calendar.MONTH) + 1
				&& day   == today.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Returns {@code true} if the given calendar represents today's date.
	 *
	 * @param calendar the calendar to test; must not be {@code null}
	 * @return {@code true} if {@code calendar} falls on today's date
	 * @see #isToday(int, int, int)
	 */
	public static boolean isToday(Calendar calendar) {
		Calendar today = Calendar.getInstance();
		return calendar.get(Calendar.YEAR)         == today.get(Calendar.YEAR)
				&& calendar.get(Calendar.MONTH)        == today.get(Calendar.MONTH)
				&& calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Returns {@code true} if the given calendar represents a future instant (after now).
	 *
	 * @param calendar the calendar to test; must not be {@code null}
	 * @return {@code true} if {@code calendar} is strictly after the current instant
	 */
	public static boolean isFuture(Calendar calendar) {
		return calendar.getTimeInMillis() > System.currentTimeMillis();
	}

	/**
	 * Returns {@code true} if the given calendar represents a past instant (before now).
	 *
	 * @param calendar the calendar to test; must not be {@code null}
	 * @return {@code true} if {@code calendar} is strictly before the current instant
	 */
	public static boolean isPast(Calendar calendar) {
		return calendar.getTimeInMillis() < System.currentTimeMillis();
	}

	/**
	 * Returns the full name of the given month in the default locale.
	 *
	 * <p>The {@code month} parameter is 1-based (1 = January, 12 = December).
	 *
	 * @param month the month, 1-based (1–12)
	 * @return the full month name (e.g. {@code "January"})
	 * @see #getMonthAsString(int, Locale)
	 */
	public static String getMonthAsString(int month) {
		return getMonthAsString(month, Locale.getDefault());
	}

	/**
	 * Returns the full name of the given month in the given locale.
	 *
	 * <p>The {@code month} parameter is 1-based (1 = January, 12 = December).
	 *
	 * @param month  the month, 1-based (1–12)
	 * @param locale the locale to use for the month name; must not be {@code null}
	 * @return the full month name in the given locale (e.g. {@code "janvier"} for French)
	 */
	public static String getMonthAsString(int month, Locale locale) {
		return new DateFormatSymbols(locale).getMonths()[month - 1];
	}

	/**
	 * Returns the full name of the given weekday in the default locale.
	 *
	 * <p>The {@code weekDay} parameter follows the {@link Calendar} convention where Monday = 1 and Sunday = 7.
	 *
	 * @param weekDay the day of the week (1 = Monday, 7 = Sunday)
	 * @return the full weekday name (e.g. {@code "Monday"})
	 * @see #getWeekDayAsString(int, boolean)
	 */
	public static String getWeekDayAsString(int weekDay) {
		return getWeekDayAsString(weekDay, false);
	}

	/**
	 * Returns the name of the given weekday in the default locale, full or abbreviated.
	 *
	 * <p>The {@code weekDay} parameter follows the {@link Calendar} convention where Monday = 1 and Sunday = 7.
	 *
	 * @param weekDay the day of the week (1 = Monday, 7 = Sunday)
	 * @param isShort {@code true} to return the abbreviated name (e.g. {@code "Mon"}), {@code false} for the full name
	 * @return the weekday name
	 */
	public static String getWeekDayAsString(int weekDay, boolean isShort) {
		int index = (weekDay == 7) ? 1 : weekDay + 1;
		return isShort
				? new DateFormatSymbols().getShortWeekdays()[index]
				: new DateFormatSymbols().getWeekdays()[index];
	}

	/**
	 * Returns the number of days in the given month.
	 *
	 * <p>The {@code month} parameter is 1-based (1 = January, 12 = December). Leap years are taken into account.
	 *
	 * @param year  the year (required to handle February in leap years)
	 * @param month the month, 1-based (1–12)
	 * @return the number of days in the given month (28–31)
	 */
	public static int getNumberOfDaysOfMonth(int year, int month) {
		return new GregorianCalendar(year, month - 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	// =============================================================================================
	// Julian day / week utilities
	// =============================================================================================

	/**
	 * The Julian day number of the Unix epoch (1970-01-01), a Thursday.
	 *
	 * @see #toJulianDayNumber(int, int, int)
	 */
	private static final int EPOCH_JULIAN_DAY = toJulianDayNumber(1970, 1, 1);

	/**
	 * The Julian day number of the Monday of the week containing {@link #EPOCH_JULIAN_DAY} (1969-12-29).
	 */
	private static final int MONDAY_BEFORE_JULIAN_EPOCH = EPOCH_JULIAN_DAY - 3;

	/**
	 * Converts a proleptic Gregorian calendar date to its Julian day number.
	 *
	 * @param year  the year
	 * @param month the month, 1-based (1–12)
	 * @param day   the day of month (1–31)
	 * @return the Julian day number of the given date
	 * @see <a href="https://en.wikipedia.org/wiki/Julian_day#Julian_day_number_calculation">Julian day number calculation — Wikipedia</a>
	 */
	private static int toJulianDayNumber(int year, int month, int day) {
		int a = (14 - month) / 12;
		int y = year + 4800 - a;
		int m = month + 12 * a - 3;
		return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
	}

	/**
	 * Returns the number of weeks since {@link #EPOCH_JULIAN_DAY} (Jan 1, 1970) adjusted for the first day of week.
	 *
	 * <p>This takes a Julian day and the week start day and calculates which week since {@link #EPOCH_JULIAN_DAY} that day occurs in, starting at 0. Do <b>not</b> use this to compute the ISO week number for the year.
	 *
	 * @param julianDay      the Julian day to calculate the week number for
	 * @param firstDayOfWeek which weekday is the first day of the week, using the {@link Calendar} convention (e.g. {@link Calendar#SUNDAY})
	 * @return the number of weeks since the epoch
	 * @see #getJulianMondayFromWeeksSinceEpoch(int)
	 * @see <a href="https://en.wikipedia.org/wiki/Julian_day">Julian day — Wikipedia</a>
	 */
	public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
		int diff = Calendar.THURSDAY - firstDayOfWeek;
		if (diff < 0) {
			diff += 7;
		}
		int refDay = EPOCH_JULIAN_DAY - diff;
		return (julianDay - refDay) / 7;
	}

	/**
	 * Returns the Julian day of the Monday for the given number of weeks since the epoch.
	 *
	 * <p>This assumes that the week containing {@link #EPOCH_JULIAN_DAY} is week 0. It returns the Julian day for the Monday {@code week} weeks after the Monday of the week containing the epoch.
	 *
	 * @param week the number of weeks since the epoch
	 * @return the Julian day for the Monday of the given week since the epoch
	 * @see #getWeeksSinceEpochFromJulianDay(int, int)
	 * @see <a href="https://en.wikipedia.org/wiki/Julian_day">Julian day — Wikipedia</a>
	 */
	public static int getJulianMondayFromWeeksSinceEpoch(int week) {
		return MONDAY_BEFORE_JULIAN_EPOCH + week * 7;
	}

	/**
	 * Returns the first day of the week as a {@link Calendar} constant, based on the device's default locale.
	 *
	 * @return a {@link Calendar} day-of-week constant (e.g. {@link Calendar#MONDAY}, {@link Calendar#SUNDAY})
	 * @see Calendar#getFirstDayOfWeek()
	 */
	public static int getFirstDayOfWeek() {
		return Calendar.getInstance().getFirstDayOfWeek();
	}
}