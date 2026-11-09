package com.osimatic.core_android;

/**
 * Résultat de l'envoi d'un élément de {@link PendingJsonQueue} par son {@link PendingJsonQueue.ItemSender}.
 * Équivalent de SendOutcome côté iOS (enum à valeur associée).
 */
public final class SendOutcome {

	public enum Type {
		/** Envoi réussi : l'élément est retiré de la queue. */
		SENT,
		/** Refus définitif : l'élément est marqué {@link PendingJsonQueue#KEY_REJECTED} avec son motif, et conservé dans la queue. */
		REJECTED,
		/** Échec temporaire : la synchronisation s'arrête, l'élément et les suivants sont conservés. */
		RETRY
	}

	public final Type type;
	/** Motif du refus renvoyé par le serveur (uniquement pour REJECTED, peut être null). */
	public final String reason;

	private SendOutcome(Type type, String reason) {
		this.type = type;
		this.reason = reason;
	}

	public static SendOutcome sent() {
		return new SendOutcome(Type.SENT, null);
	}

	public static SendOutcome rejected(String reason) {
		return new SendOutcome(Type.REJECTED, reason);
	}

	public static SendOutcome retry() {
		return new SendOutcome(Type.RETRY, null);
	}
}