package com.osimatic.core_android;

import android.content.res.Resources;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class Timestamp {
	public static String getSqlDate(long timestampInMillis, String timeZone) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
		df.setTimeZone(TimeZone.getTimeZone(timeZone));
		return df.format(new java.sql.Timestamp(timestampInMillis));
		//return (new java.sql.Date(timestamp)).toString();
	}

	public static String getSqlTime(long timestampInMillis, String timeZone) {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.FRANCE);
		df.setTimeZone(TimeZone.getTimeZone(timeZone));
		return df.format(new java.sql.Timestamp(timestampInMillis));
		//return (new java.sql.Time(timestamp)).toString();
	}

	public static String getSqlDateTime(long timestampInMillis, String timeZone) {
		//SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE);
		//df.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
		//return df.format(new Timestamp(timestamp));
		return getSqlDate(timestampInMillis, timeZone) + " " + getSqlTime(timestampInMillis, timeZone);
	}

	public static Calendar toCalendar(long timestampInMillis, String timeZone) {
		//Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+2"), Locale.getDefault());
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(timeZone), Locale.getDefault());
		//Calendar cal = Calendar.getInstance(Locale.getDefault());
		//cal.setTimeZone(TimeZone.getTimeZone(timeZone));
		cal.setTimeInMillis(timestampInMillis);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}

	/*public static Calendar toLocalCalendar(long timestampInMillis, String timeZone) {
		Calendar zonedCalendar = Timestamp.toCalendar(timestampInMillis, timeZone);
		Calendar localStartDate = Calendar.getInstance();
		localStartDate.set(zonedCalendar.get(Calendar.YEAR), zonedCalendar.get(Calendar.MONTH), zonedCalendar.get(Calendar.DATE), zonedCalendar.get(Calendar.HOUR), zonedCalendar.get(Calendar.MINUTE));
		return localStartDate;
	}*/

	public static Calendar toLocalCalendar(long timestampInMillis) {
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.setTimeInMillis(timestampInMillis);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}

	public static String formatDateShort(long timestampInMillis, Locale locale, String timeZone) {
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
		//SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", locale);
		df.setTimeZone(TimeZone.getTimeZone(timeZone));
		return df.format(new java.sql.Timestamp(timestampInMillis));
	}

	public static String formatTime(long timestampInMillis, Locale locale, String timeZone) {
		DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
		//SimpleDateFormat df = new SimpleDateFormat("HH'H'mm", locale);
		df.setTimeZone(TimeZone.getTimeZone(timeZone));
		return df.format(new java.sql.Timestamp(timestampInMillis));
	}

	public static String formatDateTime(long timestampInMillis, Resources r, Locale locale, String timeZone) {
		return String.format(r.getString(R.string.formatted_date_time),
				Timestamp.formatDateShort(timestampInMillis, locale, timeZone),
				Timestamp.formatTime(timestampInMillis, locale, timeZone));
	}


	public static long make(int year, int month, int day, String timeZone) {
		Calendar c = new GregorianCalendar(year, month - 1, day);
		c.setTimeZone(TimeZone.getTimeZone(timeZone));
		return c.getTimeInMillis();
	}
	public static long make(int year, int month, int day, int hour, int minute, String timeZone) {
		Calendar c = new GregorianCalendar(year, month - 1, day, hour, minute);
		c.setTimeZone(TimeZone.getTimeZone(timeZone));
		return c.getTimeInMillis();
	}
	public static long make(int year, int month, int day, int hour, int minute, int second, String timeZone) {
		Calendar c = new GregorianCalendar(year, month - 1, day, hour, minute, second);
		c.setTimeZone(TimeZone.getTimeZone(timeZone));
		return c.getTimeInMillis();
	}

	/**
	 * Indique si un horodatage est récent : son âge doit être inférieur ou égal à maxAgeInMillis.
	 * @param timestampInMillis horodatage en millisecondes depuis l'époque (0 renvoie false)
	 * @param maxAgeInMillis âge maximal accepté, en millisecondes
	 * @return true si l'horodatage est récent
	 */
	public static boolean isWithin(long timestampInMillis, long maxAgeInMillis) {
		return System.currentTimeMillis() - timestampInMillis <= maxAgeInMillis;
	}

	public static long getFirsDayOfMonth(int year, int month, String timeZone) {
		return make(year, month, 1, timeZone);
	}
	public static long getLastDayOfMonth(int year, int month, String timeZone) {
		return make(year, month, DateTime.getNumberOfDaysOfMonth(year, month), timeZone);
	}
}
