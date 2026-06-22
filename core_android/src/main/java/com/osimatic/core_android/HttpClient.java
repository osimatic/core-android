package com.osimatic.core_android;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HttpClient {
	private static final String TAG = "HttpClient";

	// =============================================================================================
	// Configuration statique — à initialiser au démarrage de l'app (ex: après login)
	// =============================================================================================

	public interface RefreshTokenProvider {
		String get();
	}

	public interface OnSuccessRefreshTokenListener {
		void onSuccess(String newAccessToken, String newRefreshToken);
	}

	private static String authorizationToken = null;
	private static String refreshTokenUrl = null;
	private static RefreshTokenProvider getRefreshTokenCallback = null;
	private static OnSuccessRefreshTokenListener onSuccessRefreshTokenCallback = null;
	private static Runnable onInvalidRefreshTokenCallback = null;
	private static Runnable onInvalidTokenCallback = null;

	public static void setAuthorizationToken(String token) {
		authorizationToken = token;
	}

	public static String getAuthorizationToken() {
		return authorizationToken;
	}

	public static void setRefreshTokenUrl(String url) {
		refreshTokenUrl = url;
	}

	public static void setGetRefreshTokenCallback(RefreshTokenProvider callback) {
		getRefreshTokenCallback = callback;
	}

	public static void setOnSuccessRefreshTokenCallback(OnSuccessRefreshTokenListener callback) {
		onSuccessRefreshTokenCallback = callback;
	}

	public static void setOnInvalidRefreshTokenCallback(Runnable callback) {
		onInvalidRefreshTokenCallback = callback;
	}

	public static void setOnInvalidTokenCallback(Runnable callback) {
		onInvalidTokenCallback = callback;
	}

	// =============================================================================================
	// Instance
	// =============================================================================================

	private static boolean refreshTokenStarted = false;
	private static final List<Runnable> listCompleteCallbackAfterRefreshTokenFinished = new ArrayList<>();

	protected final Context context;
	private RequestQueue mRequestQueue;

	public HttpClient(Context context) {
		this.context = context;
	}

	private RequestQueue getRequestQueue() {
		if (null == mRequestQueue) {
			mRequestQueue = Volley.newRequestQueue(context);
		}
		return mRequestQueue;
	}

	// =============================================================================================
	// En-têtes HTTP
	// =============================================================================================

	// Surcharge sans Content-Type — pour les requêtes dont le Content-Type est géré ailleurs (ex: multipart)
	public static HashMap<String, String> getHttpHeaders(@Nullable HashMap<String, String> additionalHeaders, @Nullable String accessToken) {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept-Language", Locale.getDefault().toLanguageTag());
		if (null != accessToken) {
			headers.put("Authorization", "Bearer " + accessToken);
		}
		if (null != additionalHeaders) {
			for (Map.Entry<String, String> entry : additionalHeaders.entrySet()) {
				Log.d(TAG, "header " + entry.getKey() + " : " + entry.getValue());
				headers.put(entry.getKey(), entry.getValue());
			}
		}
		return headers;
	}

	public static HashMap<String, String> getHttpHeaders(String httpMethod, @Nullable HashMap<String, String> additionalHeaders, @Nullable String accessToken, boolean asJson) {
		HashMap<String, String> headers = getHttpHeaders(additionalHeaders, accessToken);
		if (asJson) {
			headers.put("Content-Type", "application/json");
		} else if (!HTTPMethod.GET.equals(httpMethod)) {
			headers.put("Content-Type", "application/x-www-form-urlencoded");
		}
		return headers;
	}

	// =============================================================================================
	// Boundary multipart
	// =============================================================================================

	private static String generateBoundary() {
		return "boundary_" + UUID.randomUUID().toString().replace("-", "");
	}

	// =============================================================================================
	// Helpers de log
	// =============================================================================================

	public static void logSuccess(String url, int responseCode, @Nullable String data) {
		Log.d(TAG, "Success " + url + " : status code ok (" + responseCode + ")" + (null != data ? " ; data : " + data : ""));
	}

	public static void logErrorDataNil(String url, int responseCode) {
		Log.e(TAG, "Error " + url + " : data null or status code not ok (" + responseCode + ")");
	}

	public static void logErrorWithData(String url, int responseCode, String data) {
		Log.e(TAG, "Error " + url + " : status code not ok (" + responseCode + ") ; data : " + data);
	}

	public static void logErrorDecodingData(String url, @Nullable Exception error) {
		if (null != error) {
			Log.e(TAG, "Error " + url + " : decoding data exception with message " + error.getMessage());
		} else {
			Log.e(TAG, "Error " + url + " : decoding data");
		}
	}

	// =============================================================================================
	// request()
	// =============================================================================================

	public void request(String tag, String method, String url, HashMap<String, String> requestParams) {
		request(tag, method, url, requestParams, null, null, null, true, 5);
	}

	public void request(String tag, String method, String url, HashMap<String, String> requestParams, HTTPResponse.SuccessListener<String> onSuccessListener) {
		request(tag, method, url, requestParams, onSuccessListener, null, null, true, 5);
	}

	public void request(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<String> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener) {
		request(tag, method, url, requestParams, onSuccessListener, onErrorListener, null, true, 5);
	}

	public void request(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<String> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders) {
		request(tag, method, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, true, 5);
	}

	public void request(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<String> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, boolean sendAuthorizationHeader) {
		request(tag, method, url, requestParams, onSuccessListener, onErrorListener, null, sendAuthorizationHeader, 5);
	}

	public void request(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<String> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders, boolean sendAuthorizationHeader) {
		request(tag, method, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, sendAuthorizationHeader, 5);
	}

	public void request(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<String> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, boolean sendAuthorizationHeader, int timeout) {
		request(tag, method, url, requestParams, onSuccessListener, onErrorListener, null, sendAuthorizationHeader, timeout);
	}

	public void request(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<String> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders, int timeout) {
		request(tag, method, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, true, timeout);
	}

	public void request(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<String> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders, boolean sendAuthorizationHeader, int timeout) {
		String urlAndQueryString = url;
		if (method.equals(HTTPMethod.GET)) {
			urlAndQueryString += (url.contains("?") ? "" : "?") + URL.buildQueryString(requestParams);
		} else {
			requestParams.values().removeAll(Collections.singleton(null));
		}

		Log.d(TAG, "URL : " + urlAndQueryString + " ; method : " + method + (!method.equals(HTTPMethod.GET) ? " ; requestParams : " + URL.buildQueryString(requestParams) : ""));

		final String finalUrl = url;
		getRequestQueue().add(
			new StringRequest(parseHttpMethod(method), urlAndQueryString,
					response -> {
						Log.d(TAG, "HTTP Success : data : " + response);
						if (null != onSuccessListener) {
							onSuccessListener.onSuccess(response);
						}
					},
					error -> _onErrorResponse(
							error,
							onErrorListener,
							sendAuthorizationHeader,
							() -> request(tag, method, finalUrl, requestParams, onSuccessListener, onErrorListener, additionalHeaders, sendAuthorizationHeader, timeout)
					)
			) {
				@Override
				protected Map<String, String> getParams() {
					if (method.equals(HTTPMethod.POST) || method.equals(HTTPMethod.PATCH) || method.equals(HTTPMethod.DELETE)) {
						return requestParams;
					}
					return new HashMap<>();
				}

				@Override
				public Map<String, String> getHeaders() {
					return HttpClient.getHttpHeaders(method, additionalHeaders, sendAuthorizationHeader ? authorizationToken : null, false);
				}
			}
			.setTag(tag)
			.setRetryPolicy(new DefaultRetryPolicy(timeout * 1000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
		);
	}

	// =============================================================================================
	// multipartRequest()
	// =============================================================================================

	public void multipartRequest(String tag, String url, HashMap<String, String> requestParams, Map<String, List<byte[]>> filesByFieldName, @Nullable HTTPResponse.SuccessListener<String> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders) {
		multipartRequest(tag, url, requestParams, filesByFieldName, onSuccessListener, onErrorListener, additionalHeaders, true);
	}

	public void multipartRequest(String tag, String url, HashMap<String, String> requestParams, Map<String, List<byte[]>> filesByFieldName, @Nullable HTTPResponse.SuccessListener<String> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders, boolean sendAuthorizationHeader) {
		final String boundary = generateBoundary();
		final String lineEnd = "\r\n";
		final String twoHyphens = "--";

		int totalFiles = 0;
		if (null != filesByFieldName) {
			for (List<byte[]> list : filesByFieldName.values()) {
				if (null != list) {
					totalFiles += list.size();
				}
			}
		}
		Log.d(TAG, "URL : " + url + " ; multipart : " + requestParams.size() + " champs texte, " + totalFiles + " fichiers");

		getRequestQueue().add(
			new Request<String>(Request.Method.POST, url,
					error -> _onErrorResponse(
							error,
							onErrorListener,
							sendAuthorizationHeader,
							() -> multipartRequest(tag, url, requestParams, filesByFieldName, onSuccessListener, onErrorListener, additionalHeaders, sendAuthorizationHeader)
					)
			) {
				@Override
				public String getBodyContentType() {
					return "multipart/form-data; boundary=" + boundary;
				}

				@Override
				public byte[] getBody() {
					ByteArrayOutputStream body = new ByteArrayOutputStream();
					try {
						// champs texte
						for (Map.Entry<String, String> entry : requestParams.entrySet()) {
							String value = (null != entry.getValue() ? entry.getValue() : "");
							body.write((twoHyphens + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
							body.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd).getBytes(StandardCharsets.UTF_8));
							body.write(("Content-Type: text/plain; charset=UTF-8" + lineEnd).getBytes(StandardCharsets.UTF_8));
							body.write(lineEnd.getBytes(StandardCharsets.UTF_8));
							body.write(value.getBytes(StandardCharsets.UTF_8));
							body.write(lineEnd.getBytes(StandardCharsets.UTF_8));
						}
						// fichiers binaires, détection MIME par fichier via magic bytes
						if (null != filesByFieldName) {
							for (Map.Entry<String, List<byte[]>> fileEntry : filesByFieldName.entrySet()) {
								String fieldName = fileEntry.getKey();
								List<byte[]> files = fileEntry.getValue();
								if (null == files) {
									continue;
								}
								int index = 0;
								for (byte[] fileBytes : files) {
									if (null == fileBytes) {
										continue;
									}
									String fileMimeType = File.detectMimeType(fileBytes);
									String filename = fieldName + "_" + index + "." + File.getFileExtension(fileMimeType);
									body.write((twoHyphens + boundary + lineEnd).getBytes(StandardCharsets.UTF_8));
									body.write(("Content-Disposition: form-data; name=\"" + fieldName + "[]\"; filename=\"" + filename + "\"" + lineEnd).getBytes(StandardCharsets.UTF_8));
									body.write(("Content-Type: " + fileMimeType + lineEnd).getBytes(StandardCharsets.UTF_8));
									body.write(("Content-Transfer-Encoding: binary" + lineEnd).getBytes(StandardCharsets.UTF_8));
									body.write(lineEnd.getBytes(StandardCharsets.UTF_8));
									body.write(fileBytes);
									body.write(lineEnd.getBytes(StandardCharsets.UTF_8));
									index++;
								}
							}
						}
						// fermeture boundary
						body.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes(StandardCharsets.UTF_8));
					} catch (IOException e) {
						Log.e(TAG, "multipartRequest getBody : " + e.getMessage());
					}
					return body.toByteArray();
				}

				@Override
				protected Response<String> parseNetworkResponse(NetworkResponse response) {
					try {
						String body = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "UTF-8"));
						return Response.success(body, HttpHeaderParser.parseCacheHeaders(response));
					} catch (Exception e) {
						return Response.error(new ParseError(e));
					}
				}

				@Override
				protected void deliverResponse(String response) {
					Log.d(TAG, "HTTP Success : data : " + response);
					if (null != onSuccessListener) {
						onSuccessListener.onSuccess(response);
					}
				}

				@Override
				public Map<String, String> getHeaders() {
					return HttpClient.getHttpHeaders(additionalHeaders, sendAuthorizationHeader ? authorizationToken : null);
				}
			}
			.setTag(tag)
			.setRetryPolicy(new DefaultRetryPolicy(30 * 1000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
		);
	}

	// =============================================================================================
	// downloadFile()
	// =============================================================================================

	public void downloadFile(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<byte[]> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders, boolean sendAuthorizationHeader) {
		getRequestQueue().add(
			new InputStreamVolleyRequest(method,
					url,
					response -> {
						Log.d(TAG, "HTTP Success : response size : " + response.length);
						if (null != onSuccessListener) {
							onSuccessListener.onSuccess(response);
						}
					},
					error -> _onErrorResponse(
							error,
							onErrorListener,
							sendAuthorizationHeader,
							() -> downloadFile(tag, method, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, sendAuthorizationHeader)
					),
					requestParams,
					HttpClient.getHttpHeaders(method, additionalHeaders, sendAuthorizationHeader ? authorizationToken : null, false))
			.setShouldCache(true)
			.setTag(tag)
			.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
		);
	}

	// =============================================================================================
	// imageRequest()
	// =============================================================================================

	public void imageRequest(String tag, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<Bitmap> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener) {
		imageRequest(tag, url, requestParams, onSuccessListener, onErrorListener, null, true);
	}

	public void imageRequest(String tag, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<Bitmap> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders) {
		imageRequest(tag, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, true);
	}

	public void imageRequest(String tag, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<Bitmap> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders, boolean sendAuthorizationHeader) {
		String urlAndQueryString = url + (url.contains("?") ? "" : "?") + URL.buildQueryString(requestParams);
		Log.d(TAG, "URL : " + url);

		getRequestQueue().add(
			new ImageRequest(urlAndQueryString,
					response -> {
						Log.d(TAG, "HTTP Success");
						if (null != onSuccessListener) {
							onSuccessListener.onSuccess(response);
						}
					},
					0,
					0,
					null,
					null,
					error -> _onErrorResponse(
							error,
							onErrorListener,
							sendAuthorizationHeader,
							() -> imageRequest(tag, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, sendAuthorizationHeader)
					)
			) {
				@Override
				public Map<String, String> getHeaders() {
					return HttpClient.getHttpHeaders(HTTPMethod.GET, additionalHeaders, sendAuthorizationHeader ? authorizationToken : null, false);
				}
			}
			.setTag(tag)
			.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
		);
	}

	// =============================================================================================
	// jsonArrayRequest()
	// =============================================================================================

	public void jsonArrayRequest(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<JSONArray> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders) {
		jsonArrayRequest(tag, method, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, true);
	}

	public void jsonArrayRequest(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<JSONArray> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders, boolean sendAuthorizationHeader) {
		String urlAndQueryString = url;
		if (method.equals(HTTPMethod.GET)) {
			urlAndQueryString += (url.contains("?") ? "" : "?") + URL.buildQueryString(requestParams);
		} else {
			requestParams.values().removeAll(Collections.singleton(null));
		}

		Log.d(TAG, "URL : " + urlAndQueryString + (!method.equals(HTTPMethod.GET) ? " ; requestParams : " + URL.buildQueryString(requestParams) : ""));

		getRequestQueue().add(
			new JsonArrayRequest(parseHttpMethod(method), url, null,
					response -> {
						Log.d(TAG, "HTTP Success : data : " + response.toString());
						if (null != onSuccessListener) {
							onSuccessListener.onSuccess(response);
						}
					},
					error -> _onErrorResponse(
							error,
							onErrorListener,
							sendAuthorizationHeader,
							() -> jsonArrayRequest(tag, method, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, sendAuthorizationHeader)
					)
			) {
				@Override
				protected Map<String, String> getParams() {
					if (method.equals(HTTPMethod.POST)) {
						return requestParams;
					}
					return new HashMap<>();
				}

				@Override
				public Map<String, String> getHeaders() {
					return HttpClient.getHttpHeaders(method, additionalHeaders, sendAuthorizationHeader ? authorizationToken : null, false);
				}
			}
			.setTag(tag)
			.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
		);
	}

	// =============================================================================================
	// jsonObjectRequest()
	// =============================================================================================

	public void jsonObjectRequest(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<JSONObject> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, boolean dataAsJson) {
		jsonObjectRequest(tag, method, url, requestParams, onSuccessListener, onErrorListener, null, true, dataAsJson);
	}

	public void jsonObjectRequest(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<JSONObject> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders, boolean dataAsJson) {
		jsonObjectRequest(tag, method, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, true, dataAsJson);
	}

	public void jsonObjectRequest(String tag, String method, String url, HashMap<String, String> requestParams, @Nullable HTTPResponse.SuccessListener<JSONObject> onSuccessListener, @Nullable HTTPResponse.ErrorListener onErrorListener, @Nullable HashMap<String, String> additionalHeaders, boolean sendAuthorizationHeader, boolean dataAsJson) {
		String urlAndQueryString = url;
		if (method.equals(HTTPMethod.GET)) {
			urlAndQueryString += (url.contains("?") ? "" : "?") + URL.buildQueryString(requestParams);
		} else {
			requestParams.values().removeAll(Collections.singleton(null));
		}

		try {
			JSONObject jsonData = new JSONObject(requestParams);

			Log.d(TAG, "URL : " + urlAndQueryString + (!method.equals(HTTPMethod.GET) ? " ; requestParams : " + (dataAsJson ? jsonData.toString() : URL.buildQueryString(requestParams)) : ""));

			getRequestQueue().add(
				new JsonObjectRequest(parseHttpMethod(method), url, dataAsJson ? jsonData : null,
						response -> {
							Log.d(TAG, "HTTP Success : data : " + response.toString());
							if (null != onSuccessListener) {
								onSuccessListener.onSuccess(response);
							}
						},
						error -> _onErrorResponse(
								error,
								onErrorListener,
								sendAuthorizationHeader,
								() -> jsonObjectRequest(tag, method, url, requestParams, onSuccessListener, onErrorListener, additionalHeaders, sendAuthorizationHeader, dataAsJson)
						)
				) {
					@Override
					protected Map<String, String> getParams() {
						if (!dataAsJson && method.equals(HTTPMethod.POST)) {
							return requestParams;
						}
						return new HashMap<>();
					}

					@Override
					public Map<String, String> getHeaders() {
						return HttpClient.getHttpHeaders(method, additionalHeaders, sendAuthorizationHeader ? authorizationToken : null, dataAsJson);
					}
				}
				.setTag(tag)
				.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
			);
		} catch (Exception e) {
			Log.e(TAG, "jsonObjectRequest error : " + e.getMessage());
		}
	}

	// =============================================================================================
	// Utilitaires statiques
	// =============================================================================================

	public static int parseHttpMethod(String method) {
		if (method.equals(HTTPMethod.POST)) {
			return Request.Method.POST;
		}
		if (method.equals(HTTPMethod.PATCH)) {
			return Request.Method.PATCH;
		}
		if (method.equals(HTTPMethod.DELETE)) {
			return Request.Method.DELETE;
		}
		return Request.Method.GET;
	}

	// =============================================================================================
	// Refresh token
	// =============================================================================================

	public void refreshToken(Runnable onComplete) {
		refreshToken(onComplete, null);
	}

	public void refreshToken(Runnable onComplete, @Nullable HTTPResponse.ErrorListener onErrorListener) {
		if (null != onComplete) {
			listCompleteCallbackAfterRefreshTokenFinished.add(onComplete);
		}

		if (refreshTokenStarted) {
			Log.d(TAG, "refreshToken -> refreshTokenStarted, mise en file d'attente");
			return;
		}

		if (null == refreshTokenUrl) {
			Log.e(TAG, "refreshTokenUrl non défini. Appeler HttpClient.setRefreshTokenUrl(url)");
			listCompleteCallbackAfterRefreshTokenFinished.clear();
			if (null != onErrorListener) {
				onErrorListener.onError(0, null);
			}
			return;
		}

		String currentRefreshToken = (null != getRefreshTokenCallback ? getRefreshTokenCallback.get() : null);
		if (null == currentRefreshToken || currentRefreshToken.isEmpty()) {
			Log.e(TAG, "getRefreshTokenCallback non défini ou vide. Appeler HttpClient.setGetRefreshTokenCallback(callback)");
			listCompleteCallbackAfterRefreshTokenFinished.clear();
			if (null != onErrorListener) {
				onErrorListener.onError(0, null);
			}
			return;
		}

		Log.d(TAG, "refreshToken -> appel URL refresh token");
		refreshTokenStarted = true;

		HashMap<String, String> requestParams = new HashMap<>();
		requestParams.put("refresh_token", currentRefreshToken);

		jsonObjectRequest("refresh_token", HTTPMethod.POST, refreshTokenUrl, requestParams,
				(JSONObject json) -> {
					try {
						String newAccessToken = json.getString("token");
						String newRefreshToken = json.getString("refresh_token");
						Log.d(TAG, "Token refresh ok. Nouveau access token : " + newAccessToken);

						authorizationToken = newAccessToken;
						if (null != onSuccessRefreshTokenCallback) {
							onSuccessRefreshTokenCallback.onSuccess(newAccessToken, newRefreshToken);
						}

						refreshTokenStarted = false;

						for (Runnable callback : listCompleteCallbackAfterRefreshTokenFinished) {
							callback.run();
						}
						listCompleteCallbackAfterRefreshTokenFinished.clear();
					} catch (JSONException e) {
						Log.e(TAG, "refreshToken decode error : " + e.getMessage());
						refreshTokenStarted = false;
						listCompleteCallbackAfterRefreshTokenFinished.clear();
						if (null != onInvalidRefreshTokenCallback) {
							onInvalidRefreshTokenCallback.run();
						}
					}
				},
				(int httpResponseCode, String errorData) -> {
					Log.d(TAG, "Token refresh failed.");
					refreshTokenStarted = false;
					listCompleteCallbackAfterRefreshTokenFinished.clear();
					if (null != onInvalidRefreshTokenCallback) {
						onInvalidRefreshTokenCallback.run();
					}
					if (null != onErrorListener) {
						onErrorListener.onError(httpResponseCode, errorData);
					}
				},
				null,
				false, // pas de Bearer sur la requête refresh token elle-même
				true
		);
	}

	// =============================================================================================
	// Gestion des erreurs
	// =============================================================================================

	private void _onErrorResponse(VolleyError error, @Nullable HTTPResponse.ErrorListener onErrorListener, boolean sendAuthorizationHeader, @Nullable Runnable onRetryListener) {
		int responseCode = (null != error.networkResponse ? error.networkResponse.statusCode : 0);

		String errorData = null;
		if (null != error.networkResponse && null != error.networkResponse.data) {
			errorData = new String(error.networkResponse.data, StandardCharsets.UTF_8);
		}

		Log.d(TAG, "_onErrorResponse : responseCode=" + responseCode + " ; errorData=" + errorData);

		if (sendAuthorizationHeader && isExpiredToken(responseCode, errorData)) {
			if (null != onRetryListener) {
				refreshToken(onRetryListener);
			}
			return;
		}

		if (isInvalidToken(responseCode, errorData)) {
			onInvalidToken();
			if (null != onErrorListener) {
				onErrorListener.onError(responseCode, null);
			}
			return;
		}

		Log.e(TAG, "HTTP Error : code " + responseCode + " ; data : " + errorData + " ; message : " + error.getMessage());
		if (null != onErrorListener) {
			onErrorListener.onError(responseCode, errorData);
		}
	}

	public static void onInvalidToken() {
		authorizationToken = null;
		if (null != onInvalidTokenCallback) {
			onInvalidTokenCallback.run();
		}
	}

	public static boolean isExpiredToken(int responseCode, String errorData) {
		if (responseCode != 401 || null == errorData) {
			return false;
		}
		Log.d(TAG, "isExpiredToken");
		try {
			JSONObject json = new JSONObject(errorData);
			Log.d(TAG, "json : " + json);
			return (json.has("message") && "Expired JWT Token".equals(json.getString("message")))
					|| (json.has("error") && "expired_token".equals(json.getString("error")));
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isInvalidToken(int responseCode, String errorData) {
		if (responseCode != 401 || null == errorData) {
			return false;
		}
		try {
			JSONObject json = new JSONObject(errorData);
			if (json.has("message") && "Invalid JWT Token".equals(json.getString("message"))) {
				return true;
			}
			if (json.has("error")) {
				String error = json.getString("error");
				return "invalid_token".equals(error) || "authentification_failure".equals(error);
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}
}