package com.osimatic.core_android;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utility class providing helper methods for NFC (Near Field Communication) operations, including NDEF record and message creation, tag writing, and intent parsing.
 *
 * <p>Most methods require the {@code android.permission.NFC} permission to be declared in the manifest.
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see NfcAdapter
 * @see NdefMessage
 * @see NdefRecord
 * @see <a href="https://developer.android.com/guide/topics/connectivity/nfc">NFC — Android Developers</a>
 * @see <a href="https://developer.android.com/guide/topics/connectivity/nfc/nfc">NFC basics — Android Developers</a>
 */
public class NFC {

	private static final String TAG = Config.START_TAG + "NFC";

	private NFC() {}

	// =============================================================================================
	// Availability
	// =============================================================================================

	/**
	 * Returns {@code true} if the device has NFC hardware.
	 *
	 * @param context the application context; must not be {@code null}
	 * @return {@code true} if an NFC adapter is present on the device
	 * @see NfcAdapter#getDefaultAdapter(Context)
	 */
	public static boolean isNfcAvailable(Context context) {
		return NfcAdapter.getDefaultAdapter(context) != null;
	}

	/**
	 * Returns {@code true} if the device has NFC hardware and NFC is currently enabled by the user.
	 *
	 * @param context the application context; must not be {@code null}
	 * @return {@code true} if NFC is available and enabled
	 * @see NfcAdapter#isEnabled()
	 */
	public static boolean isNfcEnabled(Context context) {
		NfcAdapter adapter = NfcAdapter.getDefaultAdapter(context);
		return adapter != null && adapter.isEnabled();
	}

	// =============================================================================================
	// Record creation
	// =============================================================================================

	/**
	 * Creates an NDEF record with a custom MIME type and the given binary payload.
	 *
	 * @param mimeType the MIME type string encoded in US-ASCII (e.g. {@code "application/vnd.example"}); must not be {@code null}
	 * @param payload  the binary payload to embed in the record; must not be {@code null}
	 * @return a new {@link NdefRecord} of type {@link NdefRecord#TNF_MIME_MEDIA}
	 * @see <a href="https://developer.android.com/reference/android/nfc/NdefRecord#TNF_MIME_MEDIA">TNF_MIME_MEDIA — Android Developers</a>
	 */
	public static NdefRecord createRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(StandardCharsets.US_ASCII);
		return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
	}

	/**
	 * Creates an NDEF URI record for the given URI string.
	 *
	 * <p>The URI prefix byte is set to {@code 0x00} (no abbreviation), so the full URI string is stored in the payload.
	 *
	 * @param uri the URI string to embed (e.g. {@code "https://example.com"}); must not be {@code null}
	 * @return a new {@link NdefRecord} of type {@link NdefRecord#TNF_WELL_KNOWN} with {@link NdefRecord#RTD_URI}
	 * @see <a href="https://developer.android.com/reference/android/nfc/NdefRecord#RTD_URI">RTD_URI — Android Developers</a>
	 * @see <a href="http://members.nfc-forum.org/specs/spec_list/#uri">NFC Forum URI RTD specification</a>
	 */
	public static NdefRecord createUriRecord(String uri) {
		byte[] uriBytes = uri.getBytes(StandardCharsets.UTF_8);
		byte[] payload = new byte[1 + uriBytes.length];
		// prefix byte 0x00 = no URI prefix abbreviation
		payload[0] = 0;
		System.arraycopy(uriBytes, 0, payload, 1, uriBytes.length);
		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, null, payload);
	}

	/**
	 * Creates an NDEF text record with the given text and language.
	 *
	 * <p>The payload is encoded in UTF-8 following the NFC Forum Text RTD specification.
	 *
	 * @param text   the text to embed; must not be {@code null}
	 * @param locale the locale whose language tag is written into the record (e.g. {@link Locale#ENGLISH}); must not be {@code null}
	 * @return a new {@link NdefRecord} of type {@link NdefRecord#TNF_WELL_KNOWN} with {@link NdefRecord#RTD_TEXT}
	 * @see <a href="https://developer.android.com/reference/android/nfc/NdefRecord#RTD_TEXT">RTD_TEXT — Android Developers</a>
	 * @see <a href="http://members.nfc-forum.org/specs/spec_list/#text">NFC Forum Text RTD specification</a>
	 */
	public static NdefRecord createTextRecord(String text, Locale locale) {
		byte[] langBytes = locale.getLanguage().getBytes(StandardCharsets.US_ASCII);
		byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
		// status byte: bit 7 = 0 (UTF-8), bits 5-0 = language code length
		byte status = (byte) (langBytes.length & 0x3F);
		byte[] payload = new byte[1 + langBytes.length + textBytes.length];
		payload[0] = status;
		System.arraycopy(langBytes, 0, payload, 1, langBytes.length);
		System.arraycopy(textBytes, 0, payload, 1 + langBytes.length, textBytes.length);
		return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
	}

	// =============================================================================================
	// Message creation
	// =============================================================================================

	/**
	 * Creates an {@link NdefMessage} containing a single MIME-type NDEF record with the given payload.
	 *
	 * @param mimeType the MIME type string encoded in US-ASCII; must not be {@code null}
	 * @param payload  the binary payload to embed; must not be {@code null}
	 * @return a new {@link NdefMessage} containing a single record
	 * @see #createRecord(String, byte[])
	 */
	public static NdefMessage createMessage(String mimeType, byte[] payload) {
		return new NdefMessage(new NdefRecord[]{createRecord(mimeType, payload)});
	}

	// =============================================================================================
	// Tag writing
	// =============================================================================================

	/**
	 * Writes the given {@link NdefMessage} to the given NFC {@link Tag}.
	 *
	 * <p>If the tag already contains NDEF data ({@link Ndef}), the message is written directly. If the tag supports {@link NdefFormatable}, it is formatted and the message is written. The tag is always closed after the operation.
	 *
	 * @param message the NDEF message to write; must not be {@code null}
	 * @param tag     the NFC tag to write to; must not be {@code null}
	 * @return {@code true} if the message was written successfully, {@code false} otherwise
	 * @see Ndef
	 * @see NdefFormatable
	 * @see <a href="https://developer.android.com/guide/topics/connectivity/nfc/advanced-nfc#write-to-tag">Write to NFC tags — Android Developers</a>
	 */
	public static boolean writeTag(NdefMessage message, Tag tag) {
		int size = message.toByteArray().length;
		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				try {
					ndef.connect();
					if (!ndef.isWritable()) {
						Log.e(TAG, "Tag is not writable");
						return false;
					}
					if (ndef.getMaxSize() < size) {
						Log.e(TAG, "Message exceeds the max tag size of " + ndef.getMaxSize() + " bytes");
						return false;
					}
					ndef.writeNdefMessage(message);
					return true;
				} finally {
					try { ndef.close(); } catch (IOException ignored) {}
				}
			} else {
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					try {
						format.connect();
						format.format(message);
						return true;
					} catch (IOException e) {
						Log.e(TAG, "Failed to format and write tag", e);
						return false;
					} finally {
						try { format.close(); } catch (IOException ignored) {}
					}
				} else {
					Log.e(TAG, "Tag does not support NDEF or NdefFormatable");
					return false;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to write tag", e);
			return false;
		}
	}

	/**
	 * Returns the UID of the given NFC tag as an uppercase hex string (e.g. {@code "A1B2C3D4"}).
	 *
	 * @param tag the NFC tag to read the UID from; must not be {@code null}
	 * @return the tag UID as an uppercase hex string
	 * @see Tag#getId()
	 */
	public static String getTagId(Tag tag) {
		byte[] id = tag.getId();
		StringBuilder sb = new StringBuilder(id.length * 2);
		for (byte b : id) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}

	// =============================================================================================
	// Intent parsing
	// =============================================================================================

	/**
	 * Extracts all non-empty payload strings from NDEF messages found in the given NFC {@link Intent}.
	 *
	 * @param intent the NFC intent received in {@code onNewIntent}; must not be {@code null}
	 * @return a list of non-empty payload strings decoded as UTF-8; never {@code null}
	 * @see #getMessagesFromIntent(Intent)
	 */
	public static List<String> getStringsFromNfcIntent(Intent intent) {
		List<String> payloadStrings = new ArrayList<>();
		for (NdefMessage message : getMessagesFromIntent(intent)) {
			for (NdefRecord record : message.getRecords()) {
				String payloadString = new String(record.getPayload(), StandardCharsets.UTF_8);
				if (!TextUtils.isEmpty(payloadString)) {
					payloadStrings.add(payloadString);
				}
			}
		}
		return payloadStrings;
	}

	/**
	 * Extracts all {@link NdefMessage} objects from the given NFC {@link Intent}.
	 *
	 * <p>Handles both {@link NfcAdapter#ACTION_TAG_DISCOVERED} and {@link NfcAdapter#ACTION_NDEF_DISCOVERED} actions. If no NDEF messages are found in the extras, a single message containing an unknown-type record is returned.
	 *
	 * @param intent the NFC intent received in {@code onNewIntent}; must not be {@code null}
	 * @return a list of {@link NdefMessage} objects; never {@code null}, may be empty
	 * @see NfcAdapter#EXTRA_NDEF_MESSAGES
	 */
	public static List<NdefMessage> getMessagesFromIntent(Intent intent) {
		List<NdefMessage> intentMessages = new ArrayList<>();
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			Log.i(TAG, "Reading from NFC: " + action);
			Parcelable[] rawMsgs;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage.class);
			} else {
				//noinspection deprecation
				rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			}
			if (rawMsgs != null) {
				for (Parcelable msg : rawMsgs) {
					if (msg instanceof NdefMessage) {
						intentMessages.add((NdefMessage) msg);
					}
				}
			} else {
				// unknown tag type — wrap in an empty unknown record
				byte[] empty = new byte[0];
				NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
				intentMessages.add(new NdefMessage(new NdefRecord[]{record}));
			}
		}
		return intentMessages;
	}

	// =============================================================================================
	// Foreground dispatch
	// =============================================================================================

	/**
	 * Creates a {@link PendingIntent} suitable for enabling foreground NFC dispatch on the given activity.
	 *
	 * <p>On Android 12+ (API 31), the {@link PendingIntent#FLAG_MUTABLE} flag is set as required by the platform.
	 *
	 * @param activity the activity that will receive the NFC intent; must not be {@code null}
	 * @return a {@link PendingIntent} configured for foreground NFC dispatch
	 * @see NfcAdapter#enableForegroundDispatch(Activity, PendingIntent, android.content.IntentFilter[], String[][])
	 * @see <a href="https://developer.android.com/guide/topics/connectivity/nfc/advanced-nfc#foreground-dispatch">Foreground dispatch — Android Developers</a>
	 */
	public static PendingIntent getPendingIntent(Activity activity) {
		Intent intent = new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		int flags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			flags |= PendingIntent.FLAG_MUTABLE;
		}
		return PendingIntent.getActivity(activity, 0, intent, flags);
	}

	/**
	 * Enables foreground NFC dispatch for the given activity, using the provided {@link PendingIntent}.
	 *
	 * <p>Call this from {@link Activity#onResume()}. All NFC intents will be delivered to the activity while it is in the foreground.
	 *
	 * @param activity      the foreground activity; must not be {@code null}
	 * @param pendingIntent the pending intent to use for dispatch; must not be {@code null}
	 * @see #disableForegroundDispatch(Activity)
	 * @see <a href="https://developer.android.com/guide/topics/connectivity/nfc/advanced-nfc#foreground-dispatch">Foreground dispatch — Android Developers</a>
	 */
	public static void enableForegroundDispatch(Activity activity, PendingIntent pendingIntent) {
		NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
		if (adapter != null) {
			adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
		}
	}

	/**
	 * Disables foreground NFC dispatch for the given activity.
	 *
	 * <p>Call this from {@link Activity#onPause()} to prevent NFC intents from being delivered when the activity is not in the foreground.
	 *
	 * @param activity the foreground activity; must not be {@code null}
	 * @see #enableForegroundDispatch(Activity, PendingIntent)
	 */
	public static void disableForegroundDispatch(Activity activity) {
		NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
		if (adapter != null) {
			adapter.disableForegroundDispatch(activity);
		}
	}
}