package com.osimatic.core_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Queue générique de JSONObject persistée dans SharedPreferences.
 * Conçue pour mettre en attente des opérations hors-ligne et les rejouer au retour du réseau.
 *
 * <p>Chaque instance est liée à un nom de queue distinct, ce qui permet
 * de gérer plusieurs queues indépendantes dans la même application.</p>
 *
 * <pre>
 *   PendingJsonQueue queue = new PendingJsonQueue(context, "pending_items");
 *   queue.enqueue(item);
 *   List&lt;JSONObject&gt; items = queue.getAll();
 *   queue.removeFirst();
 * </pre>
 */
public class PendingJsonQueue {

	private static final String TAG = Config.START_TAG + "PendingJsonQueue";
	private static final String KEY_QUEUE = "queue";

	/** Clé JSON marquant un élément refusé par le serveur : il reste dans la queue sans être renvoyé. */
	public static final String KEY_REJECTED = "rejected";

	/** Clé JSON du motif de refus renvoyé par le serveur, présente uniquement sur un élément marqué {@link #KEY_REJECTED}. */
	public static final String KEY_REJECTION_REASON = "rejection_reason";

	/** Envoie un élément de la queue et retourne son résultat. */
	public interface ItemSender {
		SendOutcome send(JSONObject item);
	}

	private final Context context;
	private final String name;

	/**
	 * @param context contexte Android
	 * @param name    identifiant unique de cette queue (utilisé comme nom de SharedPreferences)
	 */
	public PendingJsonQueue(Context context, String name) {
		this.context = context.getApplicationContext();
		this.name = name;
	}

	/** Ajoute un item à la queue. */
	public void enqueue(JSONObject item) {
		JSONArray queue = load();
		queue.put(item);
		save(queue);
		Log.d(TAG, "[" + name + "] item ajouté (total : " + queue.length() + ")");
	}

	/** Retourne le nombre d'items en attente. */
	public int count() {
		return load().length();
	}

	/** Retourne true si la queue est vide. */
	public boolean isEmpty() {
		return count() == 0;
	}

	/** Retourne tous les items en attente. */
	public List<JSONObject> getAll() {
		JSONArray queue = load();
		List<JSONObject> list = new ArrayList<>();
		for (int i = 0; i < queue.length(); i++) {
			try {
				list.add(queue.getJSONObject(i));
			} catch (JSONException e) {
				Log.e(TAG, "[" + name + "] erreur lecture item " + i + " : " + e.getMessage());
			}
		}
		return list;
	}

	/** Supprime le premier item (après traitement réussi). */
	public void removeFirst() {
		JSONArray queue = load();
		if (queue.length() == 0) return;
		JSONArray newQueue = new JSONArray();
		for (int i = 1; i < queue.length(); i++) {
			try {
				newQueue.put(queue.get(i));
			} catch (JSONException e) {
				Log.e(TAG, "[" + name + "] erreur purge queue : " + e.getMessage());
			}
		}
		save(newQueue);
	}

	/** Vide entièrement la queue. */
	public void clearAll() {
		save(new JSONArray());
	}

	/**
	 * Envoie les éléments dans l'ordre, de manière synchrone.
	 *
	 * <p>Les éléments déjà marqués {@link #KEY_REJECTED} sont conservés sans être envoyés.
	 * Après un résultat {@link SendOutcome.Type#RETRY}, les éléments suivants sont conservés sans être envoyés.
	 * Les éléments ajoutés pendant la synchronisation sont conservés à la fin de la queue.</p>
	 *
	 * @param sender envoie chaque élément et retourne son résultat
	 * @return true si toute la queue a été traitée, false si une nouvelle tentative est nécessaire
	 */
	public boolean drain(ItemSender sender) {
		List<JSONObject> snapshot = getAll();
		List<JSONObject> updated = new ArrayList<>();
		boolean retry = false;

		for (JSONObject item : snapshot) {
			if (retry || item.optBoolean(KEY_REJECTED, false)) {
				updated.add(item);
				continue;
			}

			SendOutcome outcome = sender.send(item);
			if (SendOutcome.Type.RETRY == outcome.type) {
				retry = true;
				updated.add(item);
			} else if (SendOutcome.Type.REJECTED == outcome.type) {
				markAsRejected(item, outcome.reason);
				updated.add(item);
			}
			// SENT : l'élément n'est pas reconservé
		}

		replaceHead(snapshot.size(), updated);
		return !retry;
	}

	// --- Helpers ---

	private static void markAsRejected(JSONObject item, String reason) {
		try {
			item.put(KEY_REJECTED, true);
			if (null != reason) {
				item.put(KEY_REJECTION_REASON, reason);
			}
		} catch (JSONException e) {
			Log.e(TAG, "impossible de marquer l'élément refusé : " + e.getMessage());
		}
	}

	/**
	 * Remplace les {@code processedCount} premiers éléments par {@code updated}, en conservant les éléments ajoutés depuis.
	 */
	private void replaceHead(int processedCount, List<JSONObject> updated) {
		JSONArray current = load();
		JSONArray rebuilt = new JSONArray();
		for (JSONObject item : updated) {
			rebuilt.put(item);
		}
		for (int i = Math.min(processedCount, current.length()); i < current.length(); i++) {
			rebuilt.put(current.opt(i));
		}
		save(rebuilt);
	}

	private JSONArray load() {
		SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		String raw = prefs.getString(KEY_QUEUE, "[]");
		try {
			return new JSONArray(raw);
		} catch (JSONException e) {
			Log.e(TAG, "[" + name + "] queue corrompue, réinitialisation : " + e.getMessage());
			return new JSONArray();
		}
	}

	private void save(JSONArray queue) {
		SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
		prefs.edit().putString(KEY_QUEUE, queue.toString()).apply();
	}
}