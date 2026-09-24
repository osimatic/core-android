package com.osimatic.core_android;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;

/**
 * Utility class for showing a {@link MaterialTimePicker} bound to a {@link Calendar}.
 *
 * @see MaterialTimePicker
 */
public class TimePickerHelper {

	private TimePickerHelper() {}

	/**
	 * Shows a {@link MaterialTimePicker} initialized on the hour/minute of {@code time}, and updates {@code time} in place when a time is picked.
	 *
	 * <p>The year/month/day fields of {@code time} are reset from {@code referenceDay} before the picked hour/minute are applied, so {@code time} always ends up on the same day as {@code referenceDay} (which may be {@code time} itself).
	 *
	 * @param fragmentManager the {@link FragmentManager} used to show the picker dialog; must not be {@code null}
	 * @param time            the {@link Calendar} to initialize the picker with and to update in place; must not be {@code null}
	 * @param referenceDay    the {@link Calendar} whose year/month/day are copied onto {@code time}; may be {@code time} itself; must not be {@code null}
	 * @param onTimeSelected  called after {@code time} has been updated; must not be {@code null}
	 */
	public static void show(FragmentManager fragmentManager, Calendar time, Calendar referenceDay, Runnable onTimeSelected) {
		MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
				.setTimeFormat(TimeFormat.CLOCK_24H)
				.setHour(time.get(Calendar.HOUR_OF_DAY))
				.setMinute(time.get(Calendar.MINUTE))
				.setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
				.build();

		timePicker.addOnPositiveButtonClickListener(v -> {
			time.set(referenceDay.get(Calendar.YEAR), referenceDay.get(Calendar.MONTH), referenceDay.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
			time.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
			time.set(Calendar.MINUTE, timePicker.getMinute());
			onTimeSelected.run();
		});
		timePicker.addOnNegativeButtonClickListener(v -> timePicker.dismiss());
		timePicker.show(fragmentManager, "");
	}
}