package com.osimatic.core_android;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public class Password {

	public static void attachStrengthIndicator(EditText passwordField, View strengthLayout, Context context) {
		passwordField.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void afterTextChanged(Editable s) {
				updateStrengthIndicator(s.toString(), strengthLayout, context);
			}
		});
	}

	private static void updateStrengthIndicator(String password, View strengthLayout, Context context) {
		if (password.isEmpty()) {
			strengthLayout.setVisibility(View.GONE);
			return;
		}
		strengthLayout.setVisibility(View.VISIBLE);
		updateCondition(strengthLayout.findViewById(R.id.passwordCondLength), password.length() >= 8, context);
		updateCondition(strengthLayout.findViewById(R.id.passwordCondUppercase), password.matches(".*[A-Z].*"), context);
		updateCondition(strengthLayout.findViewById(R.id.passwordCondLowercase), password.matches(".*[a-z].*"), context);
		updateCondition(strengthLayout.findViewById(R.id.passwordCondNumber), password.matches(".*[0-9].*"), context);
		updateCondition(strengthLayout.findViewById(R.id.passwordCondSpecial), password.matches(".*[^A-Za-z0-9].*"), context);
	}

	private static void updateCondition(TextView tv, boolean met, Context context) {
		if (null == tv.getTag()) {
			tv.setTag(tv.getText().toString());
		}
		tv.setText((met ? "✔ " : "✗ ") + tv.getTag());
		tv.setTextColor(ContextCompat.getColor(context, met ? R.color.success_color : R.color.danger_color));
	}
}