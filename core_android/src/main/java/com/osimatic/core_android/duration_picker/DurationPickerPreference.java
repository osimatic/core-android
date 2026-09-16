package com.osimatic.core_android.duration_picker;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceFragmentCompat;

/**
 * A preference that allows the user to pick a time duration using a {@link DurationPicker}.
 * <p>
 * Use this like every other preference (for example a {@code EditTextPreference}) in your preference XML file, but
 * be aware of the following:
 * <ol>
 * <li>The {@code android:defaultValue} specifies the default duration in seconds.
 * <li>You can use one of the {@code PLACEHOLDER_*} strings in your summary which will be replaced by the duration.
 * For example a summary could look like {@code "Remind me in ${m:ss} minute(s)."}
 * </ol>
 * <p>
 * As with any other {@link DialogPreference} subclass in AndroidX, the hosting {@link PreferenceFragmentCompat} must
 * override {@code onDisplayPreferenceDialog} to show the associated {@link DurationPickerPreferenceDialogFragment}:
 * <pre>{@code
 * @Override
 * public void onDisplayPreferenceDialog(Preference preference) {
 *     if (preference instanceof DurationPickerPreference) {
 *         DurationPickerPreferenceDialogFragment fragment = DurationPickerPreferenceDialogFragment.newInstance(preference.getKey());
 *         fragment.setTargetFragment(this, 0);
 *         fragment.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
 *     } else {
 *         super.onDisplayPreferenceDialog(preference);
 *     }
 * }
 * }</pre>
 *
 * @see DurationPicker
 * @see DurationPickerPreferenceDialogFragment
 */
public class DurationPickerPreference extends DialogPreference {
	/**
	 * Placeholder in the summary that will be replaced by the current duration value.
	 */
	public static final String PLACEHOLDER_HOURS_MINUTES_SECONDS = "${h:mm:ss}";
	/**
	 * Placeholder in the summary that will be replaced by the current duration value.
	 */
	public static final String PLACEHOLDER_MINUTES_SECONDS = "${m:ss}";
	/**
	 * Placeholder in the summary that will be replaced by the current duration value.
	 */
	public static final String PLACEHOLDER_SECONDS = "${s}";

	private long duration = 0;
	private String summaryTemplate;

	public DurationPickerPreference(Context context) {
		this(context, null);
	}

	public DurationPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setPositiveButtonText(android.R.string.ok);
		setNegativeButtonText(android.R.string.cancel);
	}

	/**
	 * Set the current duration.
	 *
	 * @param duration duration in seconds
	 */
	public void setDuration(long duration) {
		this.duration = duration;
		persistLong(duration);
		notifyDependencyChange(shouldDisableDependents());
		notifyChanged();
		updateSummary();
	}

	/**
	 * Get the current duration.
	 *
	 * @return duration in seconds.
	 */
	public long getDuration() {
		return duration;
	}

	//
	// internal stuff
	//

	/**
	 * Updates the displayed summary by substituting the {@code PLACEHOLDER_*} tokens in the summary template with the current duration.
	 */
	private void updateSummary() {
		if (summaryTemplate == null) {
			CharSequence summary = getSummary();
			summaryTemplate = summary == null ? "" : summary.toString();
		}
		final String summary = summaryTemplate
				.replace(PLACEHOLDER_HOURS_MINUTES_SECONDS, DurationUtils.formatHoursMinutesSeconds(duration))
				.replace(PLACEHOLDER_MINUTES_SECONDS, DurationUtils.formatMinutesSeconds(duration)
						.replace(PLACEHOLDER_SECONDS, DurationUtils.formatSeconds(duration)));
		setSummary(summary);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return (long) a.getInt(index, 0);
	}

	@Override
	protected void onSetInitialValue(Object defaultValue) {
		long defaultDuration = defaultValue != null ? Long.parseLong(defaultValue.toString()) : 0L;
		setDuration(getPersistedLong(defaultDuration));
	}
}