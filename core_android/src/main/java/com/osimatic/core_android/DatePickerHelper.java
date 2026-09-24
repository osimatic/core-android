package com.osimatic.core_android;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;

/**
 * Utility class for showing a {@link MaterialDatePicker} bound to a {@link Calendar}, handling the UTC-midnight-to-local-date conversion safely.
 *
 * @see MaterialDatePicker
 */
public class DatePickerHelper {

	private DatePickerHelper() {}

	/**
	 * Shows a {@link MaterialDatePicker} initialized on {@code date}, and updates {@code date} in place (year/month/day fields only) when a date is picked.
	 *
	 * <p>{@link MaterialDatePicker} always returns the selection as UTC midnight of the picked day. Only the year/month/day fields are copied onto {@code date}, so its existing time zone, hour, minute, etc. are preserved and no timezone shift is introduced.
	 *
	 * @param fragmentManager the {@link FragmentManager} used to show the picker dialog; must not be {@code null}
	 * @param date            the {@link Calendar} to initialize the picker with and to update in place; must not be {@code null}
	 * @param onDateSelected  called after {@code date} has been updated; must not be {@code null}
	 */
	public static void show(FragmentManager fragmentManager, Calendar date, Runnable onDateSelected) {
		MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
				.setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
				.setSelection(date.getTimeInMillis())
				.build();

		datePicker.addOnPositiveButtonClickListener(selection -> {
			Calendar selectedDate = Timestamp.toCalendar(selection, "UTC");
			date.set(Calendar.YEAR, selectedDate.get(Calendar.YEAR));
			date.set(Calendar.MONTH, selectedDate.get(Calendar.MONTH));
			date.set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH));
			onDateSelected.run();
		});
		datePicker.addOnNegativeButtonClickListener(v -> datePicker.dismiss());
		datePicker.show(fragmentManager, "");
	}
}