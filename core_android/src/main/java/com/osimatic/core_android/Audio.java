package com.osimatic.core_android;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import java.io.IOException;

/**
 * Utility class providing helper methods for audio playback, ringer mode checks, and device vibration.
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see android.media.AudioManager
 * @see android.media.MediaPlayer
 * @see android.os.Vibrator
 */
public class Audio {

	private static final String TAG = Config.START_TAG + "Audio";

	private Audio() {}

	// =============================================================================================
	// Enums
	// =============================================================================================

	/**
	 * Controls whether the device vibrates when a sound, beep, or tone is played.
	 *
	 * @see #playSound(Context, int, VibrateOption)
	 * @see #playSound(Context, String, VibrateOption)
	 * @see #playBeep(Context, VibrateOption)
	 * @see #playAck(Context, VibrateOption)
	 */
	public enum VibrateOption {
		/** Never vibrate. */
		NEVER,
		/** Vibrate only if the ringer is in vibrate mode ({@link AudioManager#RINGER_MODE_VIBRATE}). */
		IF_VIBRATE_MODE,
		/** Always vibrate, regardless of the ringer mode. */
		ALWAYS,
		/** Vibrate if the ringer is in normal or vibrate mode; do nothing in silent mode. */
		IF_NOT_SILENT
	}

	// =============================================================================================
	// Ringer mode
	// =============================================================================================

	/**
	 * Returns {@code true} if the device ringer mode is currently set to silent.
	 *
	 * @param context the application context; must not be {@code null}
	 * @return {@code true} if ringer mode is {@link AudioManager#RINGER_MODE_SILENT}
	 * @see AudioManager#getRingerMode()
	 */
	public static boolean isSilent(Context context) {
		return getRingerMode(context) == AudioManager.RINGER_MODE_SILENT;
	}

	/**
	 * Returns {@code true} if the device ringer mode is currently set to vibrate.
	 *
	 * @param context the application context; must not be {@code null}
	 * @return {@code true} if ringer mode is {@link AudioManager#RINGER_MODE_VIBRATE}
	 * @see AudioManager#getRingerMode()
	 */
	public static boolean isVibrateMode(Context context) {
		return getRingerMode(context) == AudioManager.RINGER_MODE_VIBRATE;
	}

	/**
	 * Returns {@code true} if the device ringer mode is currently set to normal.
	 *
	 * @param context the application context; must not be {@code null}
	 * @return {@code true} if ringer mode is {@link AudioManager#RINGER_MODE_NORMAL}
	 * @see AudioManager#getRingerMode()
	 */
	public static boolean isRingerMode(Context context) {
		return getRingerMode(context) == AudioManager.RINGER_MODE_NORMAL;
	}

	/**
	 * Returns the current ringer mode of the device.
	 *
	 * @param context the application context; must not be {@code null}
	 * @return one of {@link AudioManager#RINGER_MODE_NORMAL}, {@link AudioManager#RINGER_MODE_VIBRATE}, or {@link AudioManager#RINGER_MODE_SILENT}
	 * @see AudioManager#getRingerMode()
	 */
	public static int getRingerMode(Context context) {
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return am != null ? am.getRingerMode() : AudioManager.RINGER_MODE_SILENT;
	}

	// =============================================================================================
	// Volume
	// =============================================================================================

	/**
	 * Returns the current volume for the given audio stream.
	 *
	 * @param context    the application context; must not be {@code null}
	 * @param streamType the audio stream type (e.g. {@link AudioManager#STREAM_MUSIC})
	 * @return the current volume, or {@code -1} if the {@link AudioManager} is unavailable
	 * @see AudioManager#getStreamVolume(int)
	 * @see <a href="https://developer.android.com/reference/android/media/AudioManager">AudioManager — Android Docs</a>
	 */
	public static int getVolume(Context context, int streamType) {
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return am != null ? am.getStreamVolume(streamType) : -1;
	}

	/**
	 * Returns the maximum volume for the given audio stream.
	 *
	 * @param context    the application context; must not be {@code null}
	 * @param streamType the audio stream type (e.g. {@link AudioManager#STREAM_MUSIC})
	 * @return the maximum volume, or {@code -1} if the {@link AudioManager} is unavailable
	 * @see AudioManager#getStreamMaxVolume(int)
	 */
	public static int getMaxVolume(Context context, int streamType) {
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return am != null ? am.getStreamMaxVolume(streamType) : -1;
	}

	// =============================================================================================
	// Playback
	// =============================================================================================

	/**
	 * Plays the given sound resource, respecting the current ringer mode, without vibration.
	 *
	 * <p>The sound is played only if the ringer mode is {@link AudioManager#RINGER_MODE_NORMAL}. The {@link MediaPlayer} is automatically released once playback completes.
	 *
	 * @param context the application context; must not be {@code null}
	 * @param resId   the raw resource ID of the sound to play
	 * @see #playSound(Context, int, VibrateOption)
	 */
	public static void playSound(Context context, int resId) {
		playSound(context, resId, VibrateOption.NEVER);
	}

	/**
	 * Plays the given sound resource, respecting the current ringer mode, with optional vibration.
	 *
	 * <p>The sound is played only if the ringer mode is {@link AudioManager#RINGER_MODE_NORMAL}. Vibration is triggered independently based on {@code vibrateOption}: {@link VibrateOption#ALWAYS} vibrates unconditionally, {@link VibrateOption#IF_VIBRATE_MODE} vibrates only when the ringer is in vibrate mode.
	 *
	 * @param context       the application context; must not be {@code null}
	 * @param resId         the raw resource ID of the sound to play
	 * @param vibrateOption controls when the device vibrates alongside the sound
	 * @see VibrateOption
	 * @see #playSound(Context, int)
	 * @see #vibrate(Context, long)
	 */
	public static void playSound(Context context, int resId, VibrateOption vibrateOption) {
		if (isRingerMode(context)) {
			MediaPlayer mp = MediaPlayer.create(context, resId);
			if (mp == null) {
				return;
			}
			mp.setOnCompletionListener(MediaPlayer::release);
			mp.start();
		}

		vibrateIfNeeded(context, vibrateOption);
	}

	/**
	 * Plays the audio file at the given path, without vibration.
	 *
	 * @param context  the application context; must not be {@code null}
	 * @param filePath the absolute path to the audio file to play; must not be {@code null}
	 * @see #playSound(Context, String, VibrateOption)
	 */
	public static void playSound(Context context, String filePath) {
		playSound(context, filePath, VibrateOption.NEVER);
	}

	/**
	 * Plays the audio file at the given path, with optional vibration.
	 *
	 * <p>The {@link MediaPlayer} is automatically released once playback completes. If an error occurs during preparation, the player is released immediately and playback does not start. Vibration is triggered independently based on {@code vibrateOption}.
	 *
	 * @param context       the application context; must not be {@code null}
	 * @param filePath      the absolute path to the audio file to play; must not be {@code null}
	 * @param vibrateOption controls when the device vibrates alongside the sound
	 * @see VibrateOption
	 * @see #playSound(Context, String)
	 */
	public static void playSound(Context context, String filePath, VibrateOption vibrateOption) {
		MediaPlayer mp = new MediaPlayer();
		try {
			mp.setDataSource(filePath);
			mp.prepare();
		} catch (IllegalArgumentException | IllegalStateException | IOException e) {
			Log.e(TAG, "Failed to prepare MediaPlayer: " + e.getMessage());
			mp.release();
			return;
		}
		mp.setOnCompletionListener(MediaPlayer::release);
		mp.start();

		vibrateIfNeeded(context, vibrateOption);
	}

	// =============================================================================================
	// Beep
	// =============================================================================================

	/**
	 * Plays a short notification beep using {@link ToneGenerator}, without vibration.
	 *
	 * @param context the application context; must not be {@code null}
	 * @see #playBeep(Context, VibrateOption)
	 * @see ToneGenerator#TONE_PROP_BEEP
	 * @see <a href="https://developer.android.com/reference/android/media/ToneGenerator">ToneGenerator — Android Docs</a>
	 */
	public static void playBeep(Context context) {
		playBeep(context, VibrateOption.NEVER);
	}

	/**
	 * Plays a short notification beep using {@link ToneGenerator}, with optional vibration.
	 *
	 * @param context       the application context; must not be {@code null}
	 * @param vibrateOption controls when the device vibrates alongside the tone
	 * @see VibrateOption
	 * @see ToneGenerator#TONE_PROP_BEEP
	 * @see <a href="https://developer.android.com/reference/android/media/ToneGenerator">ToneGenerator — Android Docs</a>
	 */
	public static void playBeep(Context context, VibrateOption vibrateOption) {
		playTone(context, ToneGenerator.TONE_PROP_BEEP, vibrateOption);
	}

	/**
	 * Plays a short acknowledgement tone using {@link ToneGenerator}, without vibration.
	 *
	 * @param context the application context; must not be {@code null}
	 * @see #playAck(Context, VibrateOption)
	 * @see ToneGenerator#TONE_PROP_ACK
	 * @see <a href="https://developer.android.com/reference/android/media/ToneGenerator">ToneGenerator — Android Docs</a>
	 */
	public static void playAck(Context context) {
		playAck(context, VibrateOption.NEVER);
	}

	/**
	 * Plays a short acknowledgement tone using {@link ToneGenerator}, with optional vibration.
	 *
	 * @param context       the application context; must not be {@code null}
	 * @param vibrateOption controls when the device vibrates alongside the tone
	 * @see VibrateOption
	 * @see ToneGenerator#TONE_PROP_ACK
	 * @see <a href="https://developer.android.com/reference/android/media/ToneGenerator">ToneGenerator — Android Docs</a>
	 */
	public static void playAck(Context context, VibrateOption vibrateOption) {
		playTone(context, ToneGenerator.TONE_PROP_ACK, vibrateOption);
	}

	/**
	 * Plays a tone on {@link AudioManager#STREAM_NOTIFICATION} at maximum volume for 150 ms, releases the {@link ToneGenerator} after 300 ms, and optionally vibrates according to {@code vibrateOption}.
	 *
	 * <p>Does nothing if the ringer is in silent mode ({@link AudioManager#RINGER_MODE_SILENT}).
	 *
	 * @param context       the application context; must not be {@code null}
	 * @param toneType      the tone type (e.g. {@link ToneGenerator#TONE_PROP_BEEP})
	 * @param vibrateOption controls when the device vibrates alongside the tone
	 */
	private static void playTone(Context context, int toneType, VibrateOption vibrateOption) {
		if (isSilent(context)) {
			return;
		}

		if (isRingerMode(context)) {
			try {
				ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);
				toneGenerator.startTone(toneType, 150);
				new Handler(Looper.getMainLooper()).postDelayed(toneGenerator::release, 300);
			} catch (Exception e) {
				Log.e(TAG, "playTone exception: " + e.getMessage());
			}
		}
		
		vibrateIfNeeded(context, vibrateOption);
	}

	// =============================================================================================
	// Vibration
	// =============================================================================================

	/**
	 * Vibrates for 150 ms if {@code vibrateOption} conditions are met for the current ringer mode.
	 *
	 * @param context       the application context; must not be {@code null}
	 * @param vibrateOption the vibration option to evaluate
	 */
	private static void vibrateIfNeeded(Context context, VibrateOption vibrateOption) {
		if (vibrateOption == VibrateOption.ALWAYS
				|| (vibrateOption == VibrateOption.IF_VIBRATE_MODE && isVibrateMode(context))
				|| (vibrateOption == VibrateOption.IF_NOT_SILENT && !isSilent(context))) {
			vibrate(context, 150);
		}
	}

	/**
	 * Vibrates the device for the given duration in milliseconds.
	 *
	 * <p>Uses {@link VibrationEffect#createOneShot(long, int)} on API 26+, and falls back to {@link Vibrator#vibrate(long)} on older devices. On API 31+, uses {@link VibratorManager} to obtain the default vibrator.
	 *
	 * <p>Requires the {@code android.permission.VIBRATE} permission.
	 *
	 * @param context    the application context; must not be {@code null}
	 * @param durationMs the vibration duration in milliseconds; must be &gt; 0
	 * @see VibrationEffect#createOneShot(long, int)
	 * @see <a href="https://developer.android.com/reference/android/os/VibrationEffect">VibrationEffect — Android Docs</a>
	 */
	public static void vibrate(Context context, long durationMs) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
			Vibrator v = vm != null ? vm.getDefaultVibrator() : null;
			if (v != null) {
				v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
			}
			return;
		}

		vibrateLegacy(context, durationMs);
	}

	@SuppressWarnings("deprecation")
	private static void vibrateLegacy(Context context, long durationMs) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			if (v != null) {
				v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
			}
			return;
		}

		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		if (v != null) {
			v.vibrate(durationMs);
		}
	}
}