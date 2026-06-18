package com.osimatic.core_android;

import android.app.Activity;
import android.content.DialogInterface;
import android.util.Log;

public class AlertDialog {
	private static final String TAG = Config.START_TAG + "AlertDialog";

	public static void showAlert(final Activity activity, final String message) {
		showAlert(activity, message, null);
	}

	public static void showAlert(final Activity activity, final String message, DialogInterface.OnClickListener onClickListener) {
		showAlert(activity, message, onClickListener, null);
	}

	public static void showAlert(final Activity activity, final String message, DialogInterface.OnClickListener onClickListener, DialogInterface.OnClickListener onCancelListener) {
		if (null == activity) {
			Log.e(TAG, "activity null");
			return;
		}

		activity.runOnUiThread(new Runnable() {
			public void run() {
				android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
				builder.setTitle(activity.getResources().getString(R.string.error));
				builder.setMessage(message);
				builder.setCancelable(null != onCancelListener);
				builder.setPositiveButton(activity.getResources().getString(R.string.ok), null != onClickListener ? onClickListener : new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
				if (null != onCancelListener) {
					builder.setNegativeButton(activity.getResources().getString(R.string.cancel), onCancelListener);
				}

				android.app.AlertDialog alert = builder.create();
				try {
					alert.show();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
