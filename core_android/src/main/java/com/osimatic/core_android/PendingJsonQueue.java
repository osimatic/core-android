package com.osimatic.core_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

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

	/** JSON key of the time (milliseconds since epoch) at which an item was marked {@link #KEY_REJECTED}. */
	public static final String KEY_REJECTED_AT = "rejected_at";

	/** Clé JSON du nombre d'envois échoués temporairement (résultat {@link SendOutcome.Type#RETRY}) ; absente tant qu'aucun envoi n'a échoué. */
	public static final String KEY_ATTEMPTS = "attempts";

	/** Clé JSON de la date (millisecondes depuis epoch) du dernier envoi échoué temporairement. */
	public static final String KEY_LAST_ATTEMPT_AT = "last_attempt_at";

	/** Clé JSON de la cause du dernier échec temporaire (absente si l'envoi n'a pas fourni de cause). */
	public static final String KEY_LAST_ERROR = "last_error";

	/**
	 * Lock serializing every read-modify-write of a queue, so that an item enqueued while a drain rewrites the queue is not lost.
	 * Shared by all queues: operations are short and never hold the lock while sending an item.
	 */
	private static final Object LOCK = new Object();

	/** Envoie un élément de la queue et retourne son résultat. */
	public interface ItemSender {
		SendOutcome send(JSONObject item);
	}

	/** Sélectionne les éléments de la queue à traiter (équivalent de Predicate, indisponible avant l'API 24). */
	public interface ItemMatcher {
		boolean matches(JSONObject item);
	}

	/**
	 * Écouteur notifié à chaque changement de la queue (ajout, suppression, vidage, fin d'une synchronisation).
	 * Il peut être appelé depuis n'importe quel thread : pour toucher à l'UI, il doit repasser sur le thread principal (runOnUiThread, Handler...).
	 * Pendant un {@link #drain(ItemSender)}, il n'est notifié qu'une seule fois, à la fin (pas à chaque élément envoyé).
	 * Une exception levée par un écouteur est loguée et n'empêche pas la notification des autres.
	 */
	public interface ChangeListener {
		void onQueueChanged();
	}

	/** Écouteurs de changement par nom de queue : partagés par toutes les instances d'une même queue. */
	private static final ConcurrentHashMap<String, List<ChangeListener>> CHANGE_LISTENERS = new ConcurrentHashMap<>();

	/** Verrous de synchronisation par nom de queue : un seul {@link #drain(ItemSender)} à la fois, et aucune suppression pendant un drain. */
	private static final ConcurrentHashMap<String, ReentrantLock> DRAIN_LOCKS = new ConcurrentHashMap<>();

	/** Noms des queues dont un {@link #drain(ItemSender)} est en cours (distinct du verrou, qui est aussi pris par les suppressions). */
	private static final Set<String> DRAINING = ConcurrentHashMap.newKeySet();

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
		synchronized (LOCK) {
			JSONArray queue = load();
			queue.put(item);
			save(queue);
			Log.d(TAG, "[" + name + "] item ajouté (total : " + queue.length() + ")");
		}
		notifyChanged();
	}

	/**
	 * Enregistre un écouteur notifié à chaque changement de cette queue.
	 * Les écouteurs sont conservés de façon statique : ils doivent être retirés avec {@link #removeChangeListener(ChangeListener)}
	 * (par ex. dans onStop/onDestroy), sinon l'Activity ou le Fragment qui les contient reste en mémoire.
	 */
	public void addChangeListener(ChangeListener listener) {
		List<ChangeListener> listeners = CHANGE_LISTENERS.get(name);
		if (null == listeners) {
			List<ChangeListener> created = new CopyOnWriteArrayList<>();
			listeners = CHANGE_LISTENERS.putIfAbsent(name, created);
			if (null == listeners) {
				listeners = created;
			}
		}
		listeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		List<ChangeListener> listeners = CHANGE_LISTENERS.get(name);
		if (null != listeners) {
			listeners.remove(listener);
		}
	}

	/** Retourne true si une synchronisation ({@link #drain(ItemSender)}) est en cours sur cette queue. */
	public boolean isDraining() {
		return DRAINING.contains(name);
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

	/**
	 * Retourne les items sélectionnés par le matcher.
	 * Le résultat est une photo de la queue à cet instant : il peut être périmé dès le retour (ne pas supposer qu'un {@link #tryRemove(ItemMatcher)} ultérieur supprimera les mêmes éléments).
	 * Une exception levée par le matcher est propagée.
	 */
	public List<JSONObject> getAll(ItemMatcher matcher) {
		List<JSONObject> list = new ArrayList<>();
		for (JSONObject item : getAll()) {
			if (matcher.matches(item)) {
				list.add(item);
			}
		}
		return list;
	}

	/** Retourne le nombre d'items sélectionnés par le matcher (photo à cet instant, voir {@link #getAll(ItemMatcher)}). */
	public int count(ItemMatcher matcher) {
		return getAll(matcher).size();
	}

	/** Supprime le premier item (après traitement réussi). */
	public void removeFirst() {
		synchronized (LOCK) {
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
	}

	/**
	 * Supprime les éléments sélectionnés par le matcher, sans attendre : refusée si une synchronisation est en cours (elle décalerait les éléments que le drain s'apprête à remplacer).
	 * Le matcher est appelé pour chaque élément, y compris ceux qui seront supprimés (il peut y nettoyer des ressources associées).
	 *
	 * @param matcher sélectionne les éléments à supprimer
	 * @return le nombre d'éléments supprimés, ou -1 si une synchronisation est en cours (rien n'est supprimé : l'appelant peut réessayer plus tard)
	 */
	public int tryRemove(ItemMatcher matcher) {
		ReentrantLock drainLock = getDrainLock();
		if (!drainLock.tryLock()) {
			return -1;
		}
		int removed = 0;
		try {
			synchronized (LOCK) {
				JSONArray queue = load();
				JSONArray kept = new JSONArray();
				for (int i = 0; i < queue.length(); i++) {
					JSONObject item = queue.optJSONObject(i);
					if (null != item && matcher.matches(item)) {
						removed++;
					} else {
						kept.put(queue.opt(i));
					}
				}
				if (removed > 0) {
					save(kept);
					Log.d(TAG, "[" + name + "] " + removed + " item(s) supprimé(s)");
				}
			}
		} finally {
			drainLock.unlock();
		}
		if (removed > 0) {
			notifyChanged();
		}
		return removed;
	}

	/**
	 * Vide entièrement la queue.
	 * Attend la fin d'une synchronisation en cours (un drain concurrent réécrirait les éléments supprimés) : à appeler hors du thread UI.
	 */
	public void clearAll() {
		ReentrantLock drainLock = getDrainLock();
		drainLock.lock();
		try {
			save(new JSONArray());
		} finally {
			drainLock.unlock();
		}
		notifyChanged();
	}

	/** Returns the number of items marked {@link #KEY_REJECTED}. */
	public int countRejected() {
		int count = 0;
		for (JSONObject item : getAll()) {
			if (item.optBoolean(KEY_REJECTED, false)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Removes the rejected items that were marked more than {@code maxAgeMs} milliseconds ago.
	 * A rejected item without {@link #KEY_REJECTED_AT} (marked before this key existed) is kept.
	 * Waits for a {@link #drain(ItemSender)} in progress to finish (it would shift the items the drain is about to replace).
	 * The wait can be long (network calls): do not call it from the UI thread.
	 *
	 * @param maxAgeMs maximum age of a rejected item, in milliseconds
	 * @return the number of removed items
	 */
	public int removeRejected(long maxAgeMs) {
		ReentrantLock drainLock = getDrainLock();
		drainLock.lock();
		int removed = 0;
		try {
			synchronized (LOCK) {
				JSONArray queue = load();
				JSONArray kept = new JSONArray();
				long limit = System.currentTimeMillis() - maxAgeMs;
				for (int i = 0; i < queue.length(); i++) {
					JSONObject item = queue.optJSONObject(i);
					boolean isExpired = null != item && item.optBoolean(KEY_REJECTED, false) && item.has(KEY_REJECTED_AT) && item.optLong(KEY_REJECTED_AT, Long.MAX_VALUE) < limit;
					if (isExpired) {
						removed++;
					} else {
						kept.put(queue.opt(i));
					}
				}
				if (removed > 0) {
					save(kept);
					Log.d(TAG, "[" + name + "] " + removed + " item(s) refusé(s) purgé(s)");
				}
			}
		} finally {
			drainLock.unlock();
		}
		if (removed > 0) {
			notifyChanged();
		}
		return removed;
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
		// Deux drains ne s'exécutent jamais en même temps sur une même queue (un élément serait envoyé deux fois)
		ReentrantLock drainLock = getDrainLock();
		drainLock.lock();
		DRAINING.add(name);
		try {
			return drainItems(sender);
		} finally {
			DRAINING.remove(name);
			drainLock.unlock();
			notifyChanged();
		}
	}

	private boolean drainItems(ItemSender sender) {
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
				markAsRetried(item, outcome.reason);
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

	private ReentrantLock getDrainLock() {
		ReentrantLock lock = DRAIN_LOCKS.get(name);
		if (null == lock) {
			ReentrantLock created = new ReentrantLock();
			lock = DRAIN_LOCKS.putIfAbsent(name, created);
			if (null == lock) {
				lock = created;
			}
		}
		return lock;
	}

	private void notifyChanged() {
		List<ChangeListener> listeners = CHANGE_LISTENERS.get(name);
		if (null == listeners) {
			return;
		}
		for (ChangeListener listener : listeners) {
			try {
				listener.onQueueChanged();
			} catch (RuntimeException e) {
				Log.e(TAG, "[" + name + "] exception dans un écouteur de changement", e);
			}
		}
	}

	private static void markAsRejected(JSONObject item, String reason) {
		try {
			item.put(KEY_REJECTED, true);
			item.put(KEY_REJECTED_AT, System.currentTimeMillis());
			if (null != reason) {
				item.put(KEY_REJECTION_REASON, reason);
			}
		} catch (JSONException e) {
			Log.e(TAG, "impossible de marquer l'élément refusé : " + e.getMessage());
		}
	}

	private static void markAsRetried(JSONObject item, String reason) {
		try {
			item.put(KEY_ATTEMPTS, item.optInt(KEY_ATTEMPTS, 0) + 1);
			item.put(KEY_LAST_ATTEMPT_AT, System.currentTimeMillis());
			if (null != reason) {
				item.put(KEY_LAST_ERROR, reason);
			}
		} catch (JSONException e) {
			Log.e(TAG, "impossible de tracer l'échec de l'élément : " + e.getMessage());
		}
	}

	/**
	 * Remplace les {@code processedCount} premiers éléments par {@code updated}, en conservant les éléments ajoutés depuis.
	 */
	private void replaceHead(int processedCount, List<JSONObject> updated) {
		synchronized (LOCK) {
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