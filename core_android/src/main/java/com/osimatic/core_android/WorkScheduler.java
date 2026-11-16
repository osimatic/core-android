package com.osimatic.core_android;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

/**
 * Schedules background work through WorkManager.
 *
 * <p>The consumer app must declare the WorkManager dependency itself (it declares its own Worker classes):
 * it is only a compile-time dependency of this library, so a missing dependency causes a NoClassDefFoundError when this class is used.</p>
 *
 * <p>WorkManager.getInstance() throws an IllegalStateException if the default initialization is disabled in the consumer app
 * and it does not provide a {@code Configuration.Provider}.</p>
 *
 * @see <a href="https://developer.android.com/topic/libraries/architecture/workmanager">WorkManager</a>
 */
public class WorkScheduler {

	private static final String TAG = Config.START_TAG + "WorkScheduler";

	/**
	 * Schedules a unique one-time work, run as soon as the network is available.
	 * It replaces any work already scheduled under the same name, including a retry waiting for its backoff delay: new data to process must not wait for it.
	 * A work already running is also cancelled: its thread finishes the item being processed, then the new work takes over.
	 * An item in flight when the cancellation happens may be processed twice, so the server should be idempotent.
	 *
	 * @param context        Android context
	 * @param workerClass    the Worker to run
	 * @param uniqueWorkName unique name of the work (also used to cancel it)
	 */
	public static void scheduleWhenNetworkAvailable(Context context, Class<? extends ListenableWorker> workerClass, String uniqueWorkName) {
		Constraints constraints = new Constraints.Builder()
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.build();
		OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(workerClass)
				.setConstraints(constraints)
				.build();
		WorkManager.getInstance(context.getApplicationContext())
				.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request);
		Log.d(TAG, "[" + uniqueWorkName + "] work scheduled");
	}

	/**
	 * Cancels the unique work scheduled under the given name (no-op if there is none).
	 * The cancellation is asynchronous: a work already running may still finish its current execution after this call returns.
	 *
	 * @param context        Android context
	 * @param uniqueWorkName unique name given when scheduling the work
	 */
	public static void cancel(Context context, String uniqueWorkName) {
		WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(uniqueWorkName);
	}
}