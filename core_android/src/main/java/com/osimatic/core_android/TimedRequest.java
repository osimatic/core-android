package com.osimatic.core_android;

import android.os.Handler;
import android.os.Looper;

/**
 * Requête HTTP avec délai max : à l'expiration, la requête est annulée via son canceller, puis onTimeout est appelé.
 * Chaque réponse doit appeler claimResponse() : elle n'est traitée que si le délai n'a pas encore expiré.
 * Équivalent de TimedRequest côté iOS.
 */
public class TimedRequest {

	public final HttpRequestCanceller canceller = new HttpRequestCanceller();

	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable timeout;
	private boolean answered = false;

	/**
	 * Démarre le délai. Peut être appelé depuis n'importe quel thread (le timer est planifié sur le thread principal).
	 * @param delayMs délai max avant expiration, en millisecondes
	 * @param onTimeout appelé sur le thread principal à l'expiration, après annulation de la requête
	 */
	public TimedRequest(long delayMs, Runnable onTimeout) {
		timeout = () -> {
			if (answered) {
				return;
			}
			answered = true;
			canceller.cancel();
			onTimeout.run();
		};
		handler.postDelayed(timeout, delayMs);
	}

	/**
	 * À appeler à chaque réponse, sur le thread principal.
	 * @return false si le délai a déjà été traité : la réponse doit alors être ignorée
	 */
	public boolean claimResponse() {
		if (answered) {
			return false;
		}
		answered = true;
		handler.removeCallbacks(timeout);
		return true;
	}
}