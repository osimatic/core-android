package com.osimatic.core_android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class providing helper methods for {@link Bitmap} encoding, decoding, fetching, and manipulation.
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see android.graphics.Bitmap
 * @see android.graphics.BitmapFactory
 * @see <a href="https://developer.android.com/topic/performance/graphics">Handling bitmaps — Android Developers</a>
 */
public class Image {

	private static final String TAG = Config.START_TAG + "Image";

	private Image() {}

	// =============================================================================================
	// Encoding / decoding
	// =============================================================================================

	/**
	 * Encodes the given {@link Bitmap} to a Base64 string using JPEG compression at 90% quality.
	 *
	 * @param bitmap the bitmap to encode; must not be {@code null}
	 * @return a Base64-encoded JPEG string
	 * @see #encodeToBase64(Bitmap, Bitmap.CompressFormat, int)
	 * @see #decodeBase64ToBitmap(String)
	 */
	public static String encodeToBase64Jpeg(Bitmap bitmap) {
		return encodeToBase64(bitmap, Bitmap.CompressFormat.JPEG, 90);
	}

	/** @deprecated Use {@link #encodeToBase64Jpeg(Bitmap)} instead. */
	@Deprecated
	public static String encodeJpgFileToBase64String(Bitmap fileBitmap) {
		return encodeToBase64Jpeg(fileBitmap);
	}

	/**
	 * Encodes the given {@link Bitmap} to a Base64 string using PNG compression.
	 *
	 * <p>PNG is lossless; the {@code quality} parameter is ignored for PNG.
	 *
	 * @param bitmap the bitmap to encode; must not be {@code null}
	 * @return a Base64-encoded PNG string
	 * @see #encodeToBase64(Bitmap, Bitmap.CompressFormat, int)
	 * @see #decodeBase64ToBitmap(String)
	 */
	public static String encodeToBase64Png(Bitmap bitmap) {
		return encodeToBase64(bitmap, Bitmap.CompressFormat.PNG, 100);
	}

	/** @deprecated Use {@link #encodeToBase64Png(Bitmap)} instead. */
	@Deprecated
	public static String encodePngFileToBase64String(Bitmap fileBitmap) {
		return encodeToBase64Png(fileBitmap);
	}

	/**
	 * Encodes the given {@link Bitmap} to a Base64 string using the specified format and quality.
	 *
	 * @param bitmap         the bitmap to encode; must not be {@code null}
	 * @param compressFormat the compression format to use (e.g. {@link Bitmap.CompressFormat#JPEG})
	 * @param quality        the compression quality in the range [0, 100]; ignored for lossless formats such as PNG
	 * @return a Base64-encoded string of the compressed bitmap
	 * @see Bitmap#compress(Bitmap.CompressFormat, int, java.io.OutputStream)
	 * @see <a href="https://developer.android.com/reference/android/util/Base64">Base64 — Android Developers</a>
	 */
	public static String encodeToBase64(Bitmap bitmap, Bitmap.CompressFormat compressFormat, int quality) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bitmap.compress(compressFormat, quality, out);
		return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
	}

	/**
	 * Decodes a Base64-encoded string back into a {@link Bitmap}.
	 *
	 * @param base64 the Base64-encoded image string; must not be {@code null}
	 * @return the decoded {@link Bitmap}, or {@code null} if decoding fails
	 * @see #encodeToBase64Jpeg(Bitmap)
	 * @see #encodeToBase64Png(Bitmap)
	 */
	public static Bitmap decodeBase64ToBitmap(String base64) {
		byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
	}

	/**
	 * Converts the given {@link Bitmap} to a byte array using the specified format and quality.
	 *
	 * @param bitmap         the bitmap to convert; must not be {@code null}
	 * @param compressFormat the compression format to use
	 * @param quality        the compression quality in the range [0, 100]; ignored for lossless formats
	 * @return a byte array containing the compressed bitmap data
	 * @see #fromByteArray(byte[])
	 */
	public static byte[] toByteArray(Bitmap bitmap, Bitmap.CompressFormat compressFormat, int quality) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bitmap.compress(compressFormat, quality, out);
		return out.toByteArray();
	}

	/**
	 * Decodes a byte array into a {@link Bitmap}.
	 *
	 * @param data the byte array to decode; must not be {@code null}
	 * @return the decoded {@link Bitmap}, or {@code null} if decoding fails
	 * @see #toByteArray(Bitmap, Bitmap.CompressFormat, int)
	 */
	public static Bitmap fromByteArray(byte[] data) {
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}

	// =============================================================================================
	// Network fetch
	// =============================================================================================

	/**
	 * Downloads the image at the given URL and returns it as a {@link Bitmap}.
	 *
	 * <p>This method performs a network request and must not be called on the main thread.
	 *
	 * @param url the URL of the image to download; must not be {@code null}
	 * @return the downloaded {@link Bitmap}, or {@code null} if the download fails
	 * @see #fetchBitmap(String, Map)
	 */
	public static Bitmap fetchBitmap(String url) {
		return fetchBitmap(url, new HashMap<>());
	}

	/** @deprecated Use {@link #fetchBitmap(String)} instead. */
	@Deprecated
	public static Bitmap getImageBitmap(String url) {
		return fetchBitmap(url);
	}

	/**
	 * Downloads the image at the given URL with optional query parameters and returns it as a {@link Bitmap}.
	 *
	 * <p>Query parameters are appended to the URL using {@link com.osimatic.core_android.URL#buildQueryString(Map)}. This method performs a network request and must not be called on the main thread.
	 *
	 * @param url  the base URL of the image to download; must not be {@code null}
	 * @param data optional query parameters to append to the URL; may be {@code null} or empty
	 * @return the downloaded {@link Bitmap}, or {@code null} if the download fails
	 * @see com.osimatic.core_android.URL#buildQueryString(Map)
	 */
	public static Bitmap fetchBitmap(String url, Map<String, String> data) {
		Log.d(TAG, "fetchBitmap");
		Bitmap bm = null;
		try {
			String fullUrl = url + (url.contains("?") ? "" : "?") + com.osimatic.core_android.URL.buildQueryString(data);
			URLConnection conn = new URL(fullUrl).openConnection();
			conn.connect();
			try (InputStream is = conn.getInputStream();
				 BufferedInputStream bis = new BufferedInputStream(is)) {
				bm = BitmapFactory.decodeStream(bis);
			}
		} catch (IOException e) {
			Log.e(TAG, "Error fetching bitmap", e);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bm;
	}

	/** @deprecated Use {@link #fetchBitmap(String, Map)} instead. */
	@Deprecated
	public static Bitmap getImageBitmap(String url, HashMap<String, String> data) {
		return fetchBitmap(url, data);
	}

	// =============================================================================================
	// Bitmap from Intent
	// =============================================================================================

	/**
	 * Extracts a {@link Bitmap} from the result of a camera or gallery {@link Intent}.
	 *
	 * <p>If the intent contains a URI (full-size image from gallery), the bitmap is loaded via the content resolver. If the intent contains an extras bundle with a {@code "data"} key (thumbnail from camera), that bitmap is returned directly.
	 *
	 * <p>On API 28+, uses {@link ImageDecoder} instead of the deprecated {@link MediaStore.Images.Media#getBitmap}.
	 *
	 * @param intent  the result intent from {@code onActivityResult}; may be {@code null}
	 * @param context the application context; must not be {@code null}
	 * @return the extracted {@link Bitmap}, or {@code null} if extraction fails or intent is {@code null}
	 * @see <a href="https://developer.android.com/training/camera/photobasics">Taking photos — Android Developers</a>
	 */
	public static Bitmap getImageBitmap(Intent intent, Context context) {
		if (intent == null) {
			return null;
		}
		if (intent.getData() != null) {
			try {
				Uri uri = intent.getData();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
					return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri));
				} else {
					//noinspection deprecation
					return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		if (intent.getExtras() != null) {
			try {
				return (Bitmap) intent.getExtras().get("data");
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	/**
	 * Prepares a full-resolution camera {@link Intent} using a {@link FileProvider} URI as output.
	 *
	 * <p>Creates a temporary JPEG file in {@code context.getCacheDir()/cacheSubDir}, obtains a
	 * {@link FileProvider} URI for it, and sets it as {@link MediaStore#EXTRA_OUTPUT} on the intent.
	 * The returned URI must be used to read the bitmap after the camera activity completes.
	 *
	 * @param context           the context; must not be {@code null}
	 * @param intent            the {@link MediaStore#ACTION_IMAGE_CAPTURE} intent to configure; must not be {@code null}
	 * @param fileProviderAuthority the FileProvider authority declared in AndroidManifest.xml
	 * @param cacheSubDir       subdirectory name inside {@code getCacheDir()} for the temp file
	 * @return the {@link Uri} where the camera will write the photo, or {@code null} on failure
	 */
	public static Uri prepareCameraIntent(Context context, Intent intent, String fileProviderAuthority, String cacheSubDir) {
		try {
			File dir = new File(context.getCacheDir(), cacheSubDir);
			dir.mkdirs();
			File photoFile = File.createTempFile("photo_", ".jpg", dir);
			Uri photoUri = FileProvider.getUriForFile(context, fileProviderAuthority, photoFile);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
			return photoUri;
		} catch (IOException e) {
			Log.e(TAG, "Erreur création fichier photo temporaire", e);
			return null;
		}
	}

	// =============================================================================================
	// Bitmap manipulation
	// =============================================================================================

	/**
	 * Returns a scaled copy of the given bitmap at the specified dimensions.
	 *
	 * @param bitmap the source bitmap; must not be {@code null}
	 * @param width  the target width in pixels; must be &gt; 0
	 * @param height the target height in pixels; must be &gt; 0
	 * @return a new {@link Bitmap} scaled to the given dimensions
	 * @see Bitmap#createScaledBitmap(Bitmap, int, int, boolean)
	 */
	public static Bitmap resize(Bitmap bitmap, int width, int height) {
		return Bitmap.createScaledBitmap(bitmap, width, height, true);
	}

	/**
	 * Returns a rotated copy of the given bitmap.
	 *
	 * @param bitmap  the source bitmap; must not be {@code null}
	 * @param degrees the rotation angle in degrees, clockwise
	 * @return a new {@link Bitmap} rotated by the given angle
	 * @see Matrix#postRotate(float)
	 */
	public static Bitmap rotate(Bitmap bitmap, float degrees) {
		Matrix matrix = new Matrix();
		matrix.postRotate(degrees);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	}
}