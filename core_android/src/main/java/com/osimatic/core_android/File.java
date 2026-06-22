package com.osimatic.core_android;

import android.app.DownloadManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class providing helper methods for file operations: size formatting, downloading, reading, writing, and file metadata.
 *
 * <p>This class is not instantiable. All methods are static.
 *
 * @see java.io.File
 * @see android.app.DownloadManager
 */
public class File {

	private File() {}

	// =============================================================================================
	// File size
	// =============================================================================================

	/**
	 * Formats the given file size in bytes as a human-readable string with the appropriate unit (B, KB, MB, GB, TB), using localized unit labels from the given resources.
	 *
	 * <pre>
	 * formatFileSize(0, r)           = "0"
	 * formatFileSize(1024, r)        = "1 KB"
	 * formatFileSize(1536, r)        = "1.5 KB"
	 * formatFileSize(1048576, r)     = "1 MB"
	 * </pre>
	 *
	 * @param size the file size in bytes
	 * @param r    the {@link Resources} used to retrieve localized unit labels; must not be {@code null}
	 * @return a human-readable file size string
	 */
	public static String formatFileSize(long size, Resources r) {
		if (size <= 0) {
			return "0";
		}
		final String[] units = {
				r.getString(R.string.file_size_bytes),
				r.getString(R.string.file_size_kilobytes),
				r.getString(R.string.file_size_megabytes),
				r.getString(R.string.file_size_gigabytes),
				r.getString(R.string.file_size_terrabytes)
		};
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	// =============================================================================================
	// Download
	// =============================================================================================

	/**
	 * Enqueues a file download using the system {@link DownloadManager}.
	 *
	 * <p>The file is saved to the public downloads directory ({@link Environment#DIRECTORY_DOWNLOADS}). A notification is shown when the download completes.
	 *
	 * @param context     the application context; must not be {@code null}
	 * @param url         the URL of the file to download; must not be {@code null}
	 * @param httpHeaders optional HTTP headers to include in the request; may be {@code null}
	 * @param fileName    the name to use for the saved file; must not be {@code null}
	 * @param description the description shown in the notification bar during download; may be {@code null}
	 * @param mimeType    the MIME type of the file; may be {@code null}
	 * @see DownloadManager
	 * @see <a href="https://developer.android.com/reference/android/app/DownloadManager">DownloadManager — Android Developers</a>
	 */
	public static void enqueueDownload(Context context, String url, Map<String, String> httpHeaders, String fileName, String description, String mimeType) {
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
		if (description != null) {
			request.setDescription(description);
		}
		request.setTitle(fileName);
		request.setVisibleInDownloadsUi(false);
		if (mimeType != null) {
			request.setMimeType(mimeType);
		}
		if (httpHeaders != null) {
			for (Map.Entry<String, String> header : httpHeaders.entrySet()) {
				request.addRequestHeader(header.getKey(), header.getValue());
			}
		}
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

		DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		Objects.requireNonNull(manager).enqueue(request);
	}

	/** @deprecated Use {@link #enqueueDownload(Context, String, Map, String, String, String)} instead. */
	@Deprecated
	public static void downloadFile(Context context, String url, Map<String, String> httpHeaders, String fileName, String description, String mimeType) {
		enqueueDownload(context, url, httpHeaders, fileName, description, mimeType);
	}

	// =============================================================================================
	// Read
	// =============================================================================================

	/**
	 * Reads the entire content of a text file and returns it as a {@link String}.
	 *
	 * <p>Lines are joined with a newline character ({@code '\n'}). Returns {@code null} if the file cannot be read.
	 *
	 * @param filePath the absolute path to the file to read; must not be {@code null}
	 * @return the file content as a string, or {@code null} if an error occurs
	 * @see #readBinaryFile(String)
	 */
	public static String readTextFile(String filePath) {
		try {
			java.io.File file = new java.io.File(filePath);
			StringBuilder sb = new StringBuilder((int) file.length());
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line).append('\n');
				}
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Reads the entire content of a binary file and returns it as a byte array.
	 *
	 * <p>Returns {@code null} if the file cannot be read.
	 *
	 * @param filePath the absolute path to the file to read; must not be {@code null}
	 * @return the file content as a byte array, or {@code null} if an error occurs
	 * @see #readTextFile(String)
	 */
	public static byte[] readBinaryFile(String filePath) {
		try {
			java.io.File file = new java.io.File(filePath);
			byte[] data = new byte[(int) file.length()];
			try (java.io.FileInputStream input = new java.io.FileInputStream(file)) {
				//noinspection ResultOfMethodCallIgnored
				input.read(data);
			}
			return data;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// =============================================================================================
	// Write
	// =============================================================================================

	/**
	 * Writes the given text content to a file inside the app's internal storage, creating the directory if it does not exist.
	 *
	 * <p>If the file already exists, it is overwritten.
	 *
	 * @param context   the application context; must not be {@code null}
	 * @param directory the subdirectory relative to {@link Context#getFilesDir()}; must not be {@code null}
	 * @param fileName  the name of the file to write; must not be {@code null}
	 * @param data      the text content to write; must not be {@code null}
	 * @see Context#getFilesDir()
	 */
	public static void writeToInternalStorage(Context context, String directory, String fileName, String data) {
		try {
			java.io.File dir = new java.io.File(context.getFilesDir(), directory);
			if (!dir.exists() && !dir.mkdirs()) {
				return;
			}
			writeFile(new java.io.File(dir, fileName).getAbsolutePath(), data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the given text content to the specified file path, overwriting any existing content.
	 *
	 * @param filePath the absolute path to the file to write; must not be {@code null}
	 * @param data     the text content to write; must not be {@code null}
	 * @see #writeFile(String, byte[])
	 */
	public static void writeFile(String filePath, String data) {
		try (FileWriter writer = new FileWriter(filePath)) {
			writer.write(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the given binary content to the specified file path, overwriting any existing content.
	 *
	 * @param filePath the absolute path to the file to write; must not be {@code null}
	 * @param data     the binary content to write; must not be {@code null}
	 * @see #writeFile(String, String)
	 */
	public static void writeFile(String filePath, byte[] data) {
		try (FileOutputStream output = new FileOutputStream(filePath)) {
			output.write(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// =============================================================================================
	// File info / utilities
	// =============================================================================================

	/**
	 * Returns the extension of the given file name or path, without the leading dot.
	 *
	 * <pre>
	 * getExtension("photo.jpg")          = "jpg"
	 * getExtension("archive.tar.gz")     = "gz"
	 * getExtension("README")             = ""
	 * getExtension(null)                 = null
	 * </pre>
	 *
	 * @param fileName the file name or path; may be {@code null}
	 * @return the file extension (without leading dot), an empty string if none, or {@code null} if input is {@code null}
	 * @see #getNameWithoutExtension(String)
	 */
	public static String getExtension(String fileName) {
		if (fileName == null) return null;
		int dot = fileName.lastIndexOf('.');
		return dot >= 0 ? fileName.substring(dot + 1) : "";
	}

	/**
	 * Returns the file name without its extension from the given file name or path.
	 *
	 * <pre>
	 * getNameWithoutExtension("photo.jpg")       = "photo"
	 * getNameWithoutExtension("/tmp/data.csv")   = "data"
	 * getNameWithoutExtension("README")          = "README"
	 * getNameWithoutExtension(null)              = null
	 * </pre>
	 *
	 * @param fileName the file name or path; may be {@code null}
	 * @return the file name without extension, or {@code null} if input is {@code null}
	 * @see #getExtension(String)
	 */
	public static String getNameWithoutExtension(String fileName) {
		if (fileName == null) return null;
		int sep = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		String name = sep >= 0 ? fileName.substring(sep + 1) : fileName;
		int dot = name.lastIndexOf('.');
		return dot >= 0 ? name.substring(0, dot) : name;
	}

	/**
	 * Returns the MIME type associated with the given file name's extension.
	 *
	 * <pre>
	 * getMimeType("photo.jpg")   = "image/jpeg"
	 * getMimeType("data.json")   = "application/json"
	 * getMimeType("README")      = null
	 * </pre>
	 *
	 * @param fileName the file name or path; may be {@code null}
	 * @return the MIME type string, or {@code null} if none is found or input is {@code null}
	 * @see MimeTypeMap#getMimeTypeFromExtension(String)
	 */
	public static String getMimeType(String fileName) {
		String ext = getExtension(fileName);
		if (ext == null || ext.isEmpty()) return null;
		return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US));
	}

	/**
	 * Returns the MIME type of the content identified by the given {@link Uri}.
	 *
	 * @param context the application context; must not be {@code null}
	 * @param uri     the content URI to query; must not be {@code null}
	 * @return the MIME type string, or {@code null} if the type cannot be determined
	 * @see android.content.ContentResolver#getType(Uri)
	 */
	public static String getMimeType(Context context, Uri uri) {
		return context.getContentResolver().getType(uri);
	}

	/**
	 * Returns {@code true} if a file exists at the given path.
	 *
	 * @param filePath the absolute path to check; must not be {@code null}
	 * @return {@code true} if the file exists and is a regular file
	 */
	public static boolean exists(String filePath) {
		java.io.File file = new java.io.File(filePath);
		return file.exists() && file.isFile();
	}

	/**
	 * Deletes the file at the given path.
	 *
	 * @param filePath the absolute path of the file to delete; must not be {@code null}
	 * @return {@code true} if the file was successfully deleted, {@code false} otherwise
	 * @see java.io.File#delete()
	 */
	public static boolean delete(String filePath) {
		return new java.io.File(filePath).delete();
	}

	// =============================================================================================
	// MIME type detection
	// =============================================================================================

	/**
	 * Detects the MIME type of the given binary data by inspecting its magic bytes.
	 *
	 * @param data the binary content to inspect; may be {@code null}
	 * @return the detected MIME type, or {@code "application/octet-stream"} if unknown
	 */
	public static String detectMimeType(byte[] data) {
		if (null == data || data.length < 4) {
			return "application/octet-stream";
		}
		int b0 = data[0] & 0xFF;
		int b1 = data[1] & 0xFF;
		int b2 = data[2] & 0xFF;
		int b3 = data[3] & 0xFF;
		if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) {
			return "image/jpeg";
		}
		if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) {
			return "image/png";
		}
		if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) {
			return "image/gif";
		}
		if (b0 == 0x42 && b1 == 0x4D) {
			return "image/bmp";
		}
		if (b0 == 0x49 && b1 == 0x49 && b2 == 0x2A && b3 == 0x00) {
			return "image/tiff";
		}
		if (b0 == 0x4D && b1 == 0x4D && b2 == 0x00 && b3 == 0x2A) {
			return "image/tiff";
		}
		return "application/octet-stream";
	}

	/**
	 * Returns the file extension for the given MIME type.
	 *
	 * @param mimeType the MIME type string; may be {@code null}
	 * @return the file extension without a leading dot, or {@code "bin"} if unknown
	 */
	public static String getFileExtension(String mimeType) {
		if (null == mimeType) {
			return "bin";
		}
		switch (mimeType) {
			case "image/jpeg": return "jpg";
			case "image/png": return "png";
			case "image/gif": return "gif";
			case "image/bmp": return "bmp";
			case "image/tiff": return "tiff";
			case "image/webp": return "webp";
			case "video/mp4": return "mp4";
			case "video/quicktime": return "mov";
			case "audio/mpeg": return "mp3";
			case "audio/wav": return "wav";
			case "application/pdf": return "pdf";
			case "application/zip": return "zip";
			default: return "bin";
		}
	}
}