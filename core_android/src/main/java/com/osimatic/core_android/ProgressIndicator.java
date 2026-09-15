package com.osimatic.core_android;

import android.app.Activity;
import android.util.Log;
import android.view.View;

public class ProgressIndicator {
	private static final String TAG = Config.START_TAG + "ProgressIndicator";

	public static void show(final Activity activity, final View progressView) {
		if (null == activity) {
			Log.e(TAG, "activity null");
			return;
		}
		if (null == progressView) {
			Log.e(TAG, "progressView null");
			return;
		}
		activity.runOnUiThread(() -> {
			if (!activity.isFinishing()) {
				progressView.setVisibility(View.VISIBLE);
			}
		});
	}

	public static void hide(final Activity activity, final View progressView) {
		if (null == activity) {
			Log.e(TAG, "activity null");
			return;
		}
		if (null == progressView) {
			Log.e(TAG, "progressView null");
			return;
		}
		activity.runOnUiThread(() -> {
			if (!activity.isFinishing()) {
				progressView.setVisibility(View.GONE);
			}
		});
	}
}