package com.osimatic.core_android;

import androidx.annotation.Nullable;

import com.android.volley.Request;

/**
 * Annule une requête lancée par HttpClient, y compris une relance après refresh du token.
 * Équivalent de HTTPRequestCanceller côté iOS.
 *
 * <p>L'annulation appelle le callback d'erreur de la requête avec {@link #CANCELLED_CODE}, une seule fois,
 * comme iOS où onError reçoit l'erreur d'annulation. Une requête qui a déjà reçu sa réponse n'est plus rappelée.</p>
 */
public class HttpRequestCanceller {

    /** Code passé au callback d'erreur lorsque la requête est annulée. */
    public static final int CANCELLED_CODE = -1;

    private Request<?> request;
    private HTTPResponse.ErrorListener errorListener;
    private boolean cancelled = false;

    /**
     * Rattache la requête en cours. Si cancel() a déjà été appelé, la requête est annulée aussitôt et le callback est appelé.
     * @param request requête ajoutée à la file
     * @param errorListener callback d'erreur de la requête, peut être null
     */
    void attach(Request<?> request, @Nullable HTTPResponse.ErrorListener errorListener) {
        HTTPResponse.ErrorListener toNotify = null;
        synchronized (this) {
            if (cancelled) {
                request.cancel();
                toNotify = errorListener;
            } else {
                this.request = request;
                this.errorListener = errorListener;
            }
        }
        notifyCancelled(toNotify);
    }

    /**
     * Annule la requête en cours et empêche toute relance.
     * Le callback d'erreur est appelé avec {@link #CANCELLED_CODE} si la requête n'a pas encore reçu de réponse.
     */
    public void cancel() {
        Request<?> toCancel;
        HTTPResponse.ErrorListener toNotify = null;
        synchronized (this) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            toCancel = request;
            if (null != request && !request.hasHadResponseDelivered()) {
                toNotify = errorListener;
            }
            request = null;
            errorListener = null;
        }
        if (null != toCancel) {
            toCancel.cancel();
        }
        notifyCancelled(toNotify);
    }

    /**
     * @return true si la requête a été annulée
     */
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    private static void notifyCancelled(@Nullable HTTPResponse.ErrorListener errorListener) {
        if (null != errorListener) {
            errorListener.onError(CANCELLED_CODE, null);
        }
    }
}