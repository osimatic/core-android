package com.osimatic.core_android;

import com.android.volley.Request;

/**
 * Annule une requête lancée par HttpClient, y compris une relance après refresh du token.
 * Équivalent de HTTPRequestCanceller côté iOS.
 */
public class HttpRequestCanceller {

	private Request<?> request;
	private boolean cancelled = false;

	/**
	 * Rattache la requête en cours ; si cancel() a déjà été appelé, la requête est annulée aussitôt.
	 * @param request requête ajoutée à la file
	 */
	synchronized void attach(Request<?> request) {
		if (cancelled) {
			request.cancel();
			return;
		}
		this.request = request;
	}

	/**
	 * Annule la requête en cours et empêche toute relance. Les callbacks ne sont plus appelés.
	 */
	public synchronized void cancel() {
		cancelled = true;
		if (null != request) {
			request.cancel();
			request = null;
		}
	}

	/**
	 * @return true si la requête a été annulée
	 */
	public synchronized boolean isCancelled() {
		return cancelled;
	}
}