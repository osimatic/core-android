package com.osimatic.core_android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class providing helper methods for device information, app metadata, network state, and Android API level checks.
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see android.os.Build
 * @see android.net.ConnectivityManager
 * @see <a href="https://developer.android.com/reference/android/os/Build">Build — Android Developers</a>
 */
public class Device {

	private Device() {}

	// =============================================================================================
	// Device info
	// =============================================================================================

	/**
	 * Returns a stable UUID derived from the device's {@link Settings.Secure#ANDROID_ID}.
	 *
	 * <p>The UUID is generated using {@link UUID#nameUUIDFromBytes(byte[])} (type 3, MD5-based) and is consistent across calls on the same device, but may change after a factory reset.
	 *
	 * @param context the application context; must not be {@code null}
	 * @return a stable {@link UUID} identifying this device
	 * @see Settings.Secure#ANDROID_ID
	 * @see <a href="https://developer.android.com/training/articles/user-data-ids">Best practices for unique identifiers — Android Developers</a>
	 */
	public static UUID getUniqueId(Context context) {
		String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		return UUID.nameUUIDFromBytes(androidId.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Returns the device's manufacturer and model name, separated by a space (e.g. {@code "Google Pixel 7"}).
	 *
	 * @return a human-readable device name
	 * @see #getManufacturer()
	 * @see #getModel()
	 */
	public static String getDeviceName() {
		return Build.MANUFACTURER + " " + Build.MODEL;
	}

	/**
	 * Returns the device manufacturer name (e.g. {@code "Google"}, {@code "Samsung"}).
	 *
	 * @return the value of {@link Build#MANUFACTURER}
	 */
	public static String getManufacturer() {
		return Build.MANUFACTURER;
	}

	/**
	 * Returns the device model name (e.g. {@code "Pixel 7"}, {@code "SM-G991B"}).
	 *
	 * @return the value of {@link Build#MODEL}
	 */
	public static String getModel() {
		return Build.MODEL;
	}

	/**
	 * Returns {@code true} if the app is currently running on an Android emulator.
	 *
	 * <p>Detection is based on several {@link Build} fields commonly set by emulators (AOSP, Genymotion, etc.). This heuristic may produce false negatives on uncommon emulators.
	 *
	 * @return {@code true} if the current runtime environment is likely an emulator
	 * @see <a href="https://developer.android.com/studio/run/emulator">Android Emulator — Android Developers</a>
	 */
	public static boolean isEmulator() {
		return Build.FINGERPRINT.startsWith("generic")
				|| Build.FINGERPRINT.startsWith("unknown")
				|| Build.MODEL.contains("google_sdk")
				|| Build.MODEL.contains("Emulator")
				|| Build.MODEL.contains("Android SDK built for x86")
				|| Build.MANUFACTURER.contains("Genymotion")
				|| (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
				|| "google_sdk".equals(Build.PRODUCT);
	}

	/**
	 * Returns the current Android SDK version (e.g. {@code 33} for Android 13).
	 *
	 * @return the value of {@link Build.VERSION#SDK_INT}
	 * @see <a href="https://developer.android.com/tools/releases/platforms">Android API levels — Android Developers</a>
	 */
	public static int getSdkVersion() {
		return Build.VERSION.SDK_INT;
	}

	/**
	 * Returns the Android OS version name (e.g. {@code "13"}).
	 *
	 * @return the value of {@link Build.VERSION#RELEASE}
	 */
	public static String getOsVersion() {
		return Build.VERSION.RELEASE;
	}

	/**
	 * Returns the OS name, always {@code "ANDROID"}.
	 *
	 * @return {@code "ANDROID"}
	 */
	public static String getOsName() {
		return "ANDROID";
	}

	/**
	 * Returns the first non-loopback IPv4 address of the device, or the first non-loopback IPv6 address if no IPv4 address is found.
	 *
	 * <p>Returns an empty string if no suitable address is found or if an error occurs.
	 *
	 * @return the device's IP address as a string, or {@code ""} if unavailable
	 * @see java.net.NetworkInterface
	 */
	public static String getIpAddress() {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			String ipv6Fallback = null;
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (addr.isLoopbackAddress()) continue;
					String sAddr = addr.getHostAddress();
					boolean isIPv4 = sAddr != null && sAddr.indexOf(':') < 0;
					if (isIPv4) {
						return sAddr;
					} else if (ipv6Fallback == null && sAddr != null) {
						// strip IPv6 zone suffix (e.g. "fe80::1%eth0" → "FE80::1")
						int delim = sAddr.indexOf('%');
						ipv6Fallback = (delim < 0 ? sAddr : sAddr.substring(0, delim)).toUpperCase();
					}
				}
			}
			if (ipv6Fallback != null) return ipv6Fallback;
		} catch (Exception ignored) {}
		return "";
	}

	// =============================================================================================
	// App info
	// =============================================================================================

	/**
	 * Returns the version name of the application (e.g. {@code "1.2.3"}).
	 *
	 * @param context the application context; must not be {@code null}
	 * @return the application version name, or {@code ""} if it cannot be retrieved
	 * @see android.content.pm.PackageInfo#versionName
	 */
	public static String getAppVersion(Context context) {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Returns the version code of the application.
	 *
	 * <p>On API 28+, returns {@link android.content.pm.PackageInfo#getLongVersionCode()}; on earlier versions, returns the deprecated {@code versionCode} field.
	 *
	 * @param context the application context; must not be {@code null}
	 * @return the application version code, or {@code -1} if it cannot be retrieved
	 * @see android.content.pm.PackageInfo#getLongVersionCode()
	 */
	public static long getAppVersionCode(Context context) {
		try {
			android.content.pm.PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				return info.getLongVersionCode();
			} else {
				//noinspection deprecation
				return info.versionCode;
			}
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return -1;
	}

	// =============================================================================================
	// Network
	// =============================================================================================

	/**
	 * Returns {@code true} if the device currently has an active network connection.
	 *
	 * <p>Uses {@link NetworkCapabilities} to check for Wi-Fi, cellular, or Ethernet transport.
	 *
	 * <p><b>Note:</b> this method checks transport availability, not actual internet reachability.
	 *
	 * @param context the application context; must not be {@code null}
	 * @return {@code true} if an active network connection is available
	 * @see ConnectivityManager#getNetworkCapabilities(Network)
	 * @see <a href="https://developer.android.com/training/monitoring-device-state/connectivity-status-type">Monitor connectivity status — Android Developers</a>
	 */
	public static boolean isOnline(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) return false;
		Network network = cm.getActiveNetwork();
		if (network == null) return false;
		NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
		return capabilities != null
				&& (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
				|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
	}

	/** @deprecated Use {@link #isOnline(Context)} instead. */
	@Deprecated
	public static boolean isDeviceOnline(Context context) {
		return isOnline(context);
	}

	/**
	 * Listener notified when the overall network availability changes.
	 *
	 * @see #registerNetworkAvailabilityCallback(Context, NetworkAvailabilityListener)
	 */
	public interface NetworkAvailabilityListener {
		void onNetworkAvailabilityChanged(boolean available);
	}

	/**
	 * Registers a callback that notifies {@code listener} whenever the device gains or loses internet access.
	 *
	 * <p>Unlike a naive {@link ConnectivityManager.NetworkCallback}, this correctly handles devices with several active networks (e.g. Wi-Fi and cellular at the same time): availability is only reported as lost once every tracked network is gone, not on the first {@code onLost}.
	 *
	 * <p>The returned callback must be unregistered with {@link #unregisterNetworkAvailabilityCallback(Context, ConnectivityManager.NetworkCallback)} once no longer needed (e.g. in {@code onCleared()}).
	 *
	 * @param context the application context; must not be {@code null}
	 * @param listener notified with {@code true}/{@code false} on every availability change
	 * @return the registered {@link ConnectivityManager.NetworkCallback}, to be passed to {@link #unregisterNetworkAvailabilityCallback}
	 * @see <a href="https://developer.android.com/training/monitoring-device-state/connectivity-status-type">Monitor connectivity status — Android Developers</a>
	 */
	public static ConnectivityManager.NetworkCallback registerNetworkAvailabilityCallback(Context context, NetworkAvailabilityListener listener) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		// Plusieurs réseaux (wifi + data mobile) peuvent être actifs en même temps
		Set<Network> availableNetworks = Collections.synchronizedSet(new HashSet<>());
		ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
			@Override
			public void onAvailable(@NonNull Network network) {
				availableNetworks.add(network);
				listener.onNetworkAvailabilityChanged(true);
			}

			@Override
			public void onLost(@NonNull Network network) {
				availableNetworks.remove(network);
				listener.onNetworkAvailabilityChanged(!availableNetworks.isEmpty());
			}
		};
		if (cm != null) {
			NetworkRequest request = new NetworkRequest.Builder()
					.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
					.build();
			cm.registerNetworkCallback(request, callback);
		}
		return callback;
	}

	/**
	 * Unregisters a callback previously registered with {@link #registerNetworkAvailabilityCallback(Context, NetworkAvailabilityListener)}.
	 *
	 * @param context the application context; must not be {@code null}
	 * @param callback the callback returned by {@link #registerNetworkAvailabilityCallback}
	 */
	public static void unregisterNetworkAvailabilityCallback(Context context, ConnectivityManager.NetworkCallback callback) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null && callback != null) {
			cm.unregisterNetworkCallback(callback);
		}
	}

	// =============================================================================================
	// API level checks
	// =============================================================================================

	/**
	 * Returns {@code true} if the device's API level is greater than or equal to the given level.
	 *
	 * @param apiLevel the minimum API level to check (e.g. {@link Build.VERSION_CODES#M})
	 * @return {@code true} if {@link Build.VERSION#SDK_INT} ≥ {@code apiLevel}
	 */
	@ChecksSdkIntAtLeast(parameter = 0)
	public static boolean isAtLeast(int apiLevel) {
		return Build.VERSION.SDK_INT >= apiLevel;
	}

	/**
	 * Returns {@code true} if the device runs Android Oreo (API 26) or later.
	 *
	 * @return {@code true} if {@link Build.VERSION#SDK_INT} ≥ {@link Build.VERSION_CODES#O}
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
	public static boolean isOreoOrLater() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
	}

	/**
	 * Returns {@code true} if the device runs Android Pie (API 28) or later.
	 *
	 * @return {@code true} if {@link Build.VERSION#SDK_INT} ≥ {@link Build.VERSION_CODES#P}
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
	public static boolean isPieOrLater() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
	}

	/**
	 * Returns {@code true} if the device runs Android 10 (API 29) or later.
	 *
	 * @return {@code true} if {@link Build.VERSION#SDK_INT} ≥ {@link Build.VERSION_CODES#Q}
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
	public static boolean isAndroid10OrLater() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
	}

	/**
	 * Returns {@code true} if the device runs Android 11 (API 30) or later.
	 *
	 * @return {@code true} if {@link Build.VERSION#SDK_INT} ≥ {@link Build.VERSION_CODES#R}
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
	public static boolean isAndroid11OrLater() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
	}

	/**
	 * Returns {@code true} if the device runs Android 12 (API 31) or later.
	 *
	 * @return {@code true} if {@link Build.VERSION#SDK_INT} ≥ {@link Build.VERSION_CODES#S}
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
	public static boolean isAndroid12OrLater() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
	}

	/**
	 * Returns {@code true} if the device runs Android 13 (API 33) or later.
	 *
	 * @return {@code true} if {@link Build.VERSION#SDK_INT} ≥ {@link Build.VERSION_CODES#TIRAMISU}
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
	public static boolean isAndroid13OrLater() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
	}

	/**
	 * Returns {@code true} if the device runs Android 14 (API 34) or later.
	 *
	 * @return {@code true} if {@link Build.VERSION#SDK_INT} ≥ {@link Build.VERSION_CODES#UPSIDE_DOWN_CAKE}
	 */
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	public static boolean isAndroid14OrLater() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
	}

	/**
	 * @deprecated Always returns {@code true} since minSdk 21 is above Jellybean (API 16). Use {@link #isAtLeast(int)} for explicit API level checks.
	 */
	@Deprecated
	public static boolean isJellybeanOrLater() {
		return true;
	}
}