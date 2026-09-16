package com.osimatic.core_android.duration_picker;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.osimatic.core_android.R;

/**
 * The {@link PreferenceDialogFragmentCompat} shown by a {@link DurationPickerPreference}.
 *
 * @see DurationPickerPreference
 */
public class DurationPickerPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

	private DurationPicker picker;

	/**
	 * Creates a new instance of this fragment for the preference with the given key.
	 *
	 * @param key the key of the {@link DurationPickerPreference} to show; must not be {@code null}
	 * @return a new {@link DurationPickerPreferenceDialogFragment}
	 */
	public static DurationPickerPreferenceDialogFragment newInstance(String key) {
		final DurationPickerPreferenceDialogFragment fragment = new DurationPickerPreferenceDialogFragment();
		final Bundle args = new Bundle(1);
		args.putString(ARG_KEY, key);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	protected View onCreateDialogView(Context context) {
		picker = initPicker((DurationPicker) LayoutInflater.from(context).inflate(R.layout.duration_picker_dialog, null));
		return picker;
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		DialogPreference preference = getPreference();
		if (preference instanceof DurationPickerPreference) {
			picker.setDuration(((DurationPickerPreference) preference).getDuration());
		}
	}

	/**
	 * Hook for subclasses to customize the {@link DurationPicker} before it is shown.
	 *
	 * @param picker the picker that was just inflated; must not be {@code null}
	 * @return the picker to use; by default, the same instance that was passed in
	 */
	protected DurationPicker initPicker(DurationPicker picker) {
		return picker;
	}

	@Override
	public void onDialogClosed(boolean positiveResult) {
		if (!positiveResult) {
			return;
		}
		DialogPreference preference = getPreference();
		if (!(preference instanceof DurationPickerPreference)) {
			return;
		}
		final DurationPickerPreference durationPreference = (DurationPickerPreference) preference;
		final long newDuration = picker.getDuration();
		if (durationPreference.callChangeListener(newDuration)) {
			durationPreference.setDuration(newDuration);
		}
	}
}