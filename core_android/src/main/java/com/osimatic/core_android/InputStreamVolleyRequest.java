package com.osimatic.core_android;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.HashMap;
import java.util.Map;

public class InputStreamVolleyRequest extends Request<byte[]> {
	private final Response.Listener<byte[]> mListener;
	private final Map<String, String> mParams;
	private final Map<String, String> mRequestHeaders;

	public InputStreamVolleyRequest(String method, String mUrl, Response.Listener<byte[]> listener,
									Response.ErrorListener errorListener, HashMap<String, String> params,
									Map<String, String> requestHeaders) {
		super(HttpClient.parseHttpMethod(method), mUrl, errorListener);
		setShouldCache(false);
		mListener = listener;
		mParams = params;
		mRequestHeaders = requestHeaders;
	}

	@Override
	protected Map<String, String> getParams() {
		return mParams;
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		Map<String, String> headers = new HashMap<>(super.getHeaders());
		headers.putAll(mRequestHeaders);
		return headers;
	}

	@Override
	protected void deliverResponse(byte[] response) {
		mListener.onResponse(response);
	}

	@Override
	protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
		return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
	}
}