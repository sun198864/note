package com.davidsun.note.data.remote

import com.davidsun.note.core.common.AppClock
import com.davidsun.note.data.local.dao.NoteDao
import com.davidsun.note.data.local.dao.TaskDao
import com.davidsun.note.data.local.mapper.toDomain
import com.davidsun.note.data.local.mapper.toEntity
import com.davidsun.note.domain.model.CloudSyncIssue
import com.davidsun.note.domain.model.CloudSyncStatus
import com.davidsun.note.domain.model.Note
import com.davidsun.note.domain.model.SyncStatus
import com.davidsun.note.domain.model.Task
import com.davidsun.note.domain.repository.CloudSyncCoordinator
import com.davidsun.note.domain.repository.ReminderScheduler
import com.davidsun.note.domain.sync.SyncMerge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Singleton
class FirestoreCloudSync @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val reminderScheduler: Lazy<ReminderScheduler>,
    private val preferences: CloudSyncPreferences,
    private val clock: AppClock,
) : CloudSyncCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val mutex = Mutex()
    private val pushes = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _status = MutableStateFlow(CloudSyncStatus())
    override val status: StateFlow<CloudSyncStatus> = _status.asStateFlow()

    private var session: Job? = null
    @Volatile private var notesReady = false
    @Volatile private var tasksReady = false

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            val saved = preferences.lastSyncedAt()
            if (saved != null) {
                _status.update { it.copy(lastSyncedAt = saved) }
            }
        }
        auth.addAuthStateListener { firebaseAuth ->
            session?.cancel()
            session = null
            notesReady = false
            tasksReady = false
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                _status.update {
                    it.copy(
                        isSyncing = false,
                        issue = CloudSyncIssue.None,
                        switchedAccount = false,
                    )
                }
            } else {
                session = scope.launch { runSession(uid) }
            }
        }
    }

    override fun notifyLocalDataChanged() {
        pushes.tryEmit(Unit)
    }

    override suspend fun flush() {
        val uid = auth.currentUser?.uid ?: return
        if (!notesReady && !tasksReady) return
        pushPending(uid)
    }

    private suspend fun runSession(uid: String) = coroutineScope {
        prepareAccount(uid)
        var notesListener: ListenerRegistration? = null
        var tasksListener: ListenerRegistration? = null
        try {
            notesListener = listen(this, uid, NOTES) { snapshot ->
                snapshot.documents.forEach { document ->
                    document.toNote()?.let { applyNote(it) }
                }
                notesReady = true
                recordServerSnapshot(snapshot)
            }
            tasksListener = listen(this, uid, TASKS) { snapshot ->
                val applied = snapshot.documents.mapNotNull { document ->
                    document.toTask()?.takeIf { applyTask(it) }
                }
                applied.forEach { task -> reminderScheduler.get().sync(task) }
                tasksReady = true
                recordServerSnapshot(snapshot)
            }
            pushes.debounce(PUSH_DEBOUNCE_MS).collect {
                if (pushPending(uid)) {
                    delay(RETRY_DELAY_MS)
                    pushes.tryEmit(Unit)
                }
            }
        } finally {
            notesListener?.remove()
            tasksListener?.remove()
        }
    }

    private suspend fun prepareAccount(uid: String) {
        val boundUid = preferences.boundUid()
        if (boundUid != null && boundUid != uid) {
            taskDao.getAll().map { it.toDomain() }.forEach { task ->
                reminderScheduler.get().sync(task.copy(deletedAt = task.deletedAt ?: 0L))
            }
            noteDao.deleteAll()
            taskDao.deleteAll()
            _status.update { it.copy(switchedAccount = true) }
        }
        preferences.setBoundUid(uid)
    }

    private fun listen(
        scope: CoroutineScope,
        uid: String,
        collection: String,
        onSnapshot: suspend (QuerySnapshot) -> Unit,
    ): ListenerRegistration =
        firestore.collection(USERS).document(uid).collection(collection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    report(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                scope.launch {
                    mutex.withLock { onSnapshot(snapshot) }
                    pushes.tryEmit(Unit)
                }
            }

    private suspend fun pushPending(uid: String): Boolean {
        _status.update { it.copy(isSyncing = true) }
        try {
            if (notesReady) pushNotes(uid)
            if (tasksReady) pushTasks(uid)
            _status.update { it.copy(isSyncing = false, issue = CloudSyncIssue.None) }
            return false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val permission = error is FirebaseFirestoreException &&
                (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED)
            _status.update {
                it.copy(
                    isSyncing = false,
                    issue = if (permission) CloudSyncIssue.PermissionDenied else CloudSyncIssue.Retrying,
                )
            }
            return !permission
        }
    }

    private suspend fun pushNotes(uid: String) {
        val pending = noteDao.getPending().map { it.toDomain() }
        pending.forEach { note -> pushNote(uid, note) }
    }

    private suspend fun pushTasks(uid: String) {
        val pending = taskDao.getPending().map { it.toDomain() }
        pending.forEach { task -> pushTask(uid, task) }
    }

    private suspend fun pushNote(uid: String, note: Note) {
        val ref = firestore.collection(USERS).document(uid).collection(NOTES).document(note.id)
        val wrote = firestore.runTransaction { transaction ->
            val remote = transaction.get(ref).toNote()
            val remoteWins = remote != null && SyncMerge.remoteWins(
                localUpdatedAt = note.updatedAt,
                localRevision = note.revision,
                remoteUpdatedAt = remote.updatedAt,
                remoteRevision = remote.revision,
            )
            if (remoteWins) {
                false
            } else {
                transaction.set(ref, note.toFirestoreMap())
                true
            }
        }.await()
        if (wrote) {
            noteDao.markSynced(note.id, note.revision)
        } else {
            ref.get().await().toNote()?.let { applyNote(it) }
        }
    }

    private suspend fun pushTask(uid: String, task: Task) {
        val ref = firestore.collection(USERS).document(uid).collection(TASKS).document(task.id)
        val wrote = firestore.runTransaction { transaction ->
            val remote = transaction.get(ref).toTask()
            val remoteWins = remote != null && SyncMerge.remoteWins(
                localUpdatedAt = task.updatedAt,
                localRevision = task.revision,
                remoteUpdatedAt = remote.updatedAt,
                remoteRevision = remote.revision,
            )
            if (remoteWins) {
                false
            } else {
                transaction.set(ref, task.toFirestoreMap())
                true
            }
        }.await()
        if (wrote) {
            taskDao.markSynced(task.id, task.revision)
        } else {
            val remote = ref.get().await().toTask() ?: return
            if (applyTask(remote)) {
                reminderScheduler.get().sync(remote)
            }
        }
    }

    private suspend fun applyNote(remote: Note) {
        val local = noteDao.getAnyById(remote.id)?.toDomain()
        if (local != null && !SyncMerge.remoteWins(
                local.updatedAt,
                local.revision,
                remote.updatedAt,
                remote.revision,
            )
        ) {
            return
        }
        if (local?.copy(syncStatus = SyncStatus.SYNCED) == remote) {
            if (local.syncStatus != SyncStatus.SYNCED) noteDao.markSynced(local.id, local.revision)
            return
        }
        noteDao.upsert(remote.copy(syncStatus = SyncStatus.SYNCED).toEntity())
    }

    private suspend fun applyTask(remote: Task): Boolean {
        val local = taskDao.getAnyById(remote.id)?.toDomain()
        if (local != null && !SyncMerge.remoteWins(
                local.updatedAt,
                local.revision,
                remote.updatedAt,
                remote.revision,
            )
        ) {
            return false
        }
        if (local?.copy(syncStatus = SyncStatus.SYNCED) == remote) {
            if (local.syncStatus != SyncStatus.SYNCED) taskDao.markSynced(local.id, local.revision)
            return false
        }
        taskDao.upsert(remote.copy(syncStatus = SyncStatus.SYNCED).toEntity())
        return true
    }

    private suspend fun recordServerSnapshot(snapshot: QuerySnapshot) {
        if (snapshot.metadata.isFromCache) return
        val now = clock.nowMillis()
        preferences.setLastSyncedAt(now)
        _status.update { it.copy(lastSyncedAt = now, issue = CloudSyncIssue.None) }
    }

    private fun report(error: Exception) {
        if (error is CancellationException) return
        val permission = error is FirebaseFirestoreException &&
            (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                error.code == FirebaseFirestoreException.Code.UNAUTHENTICATED)
        _status.update {
            it.copy(
                isSyncing = false,
                issue = if (permission) CloudSyncIssue.PermissionDenied else CloudSyncIssue.Retrying,
            )
        }
    }

    private companion object {
        const val USERS = "users"
        const val NOTES = "notes"
        const val TASKS = "tasks"
        const val PUSH_DEBOUNCE_MS = 500L
        const val RETRY_DELAY_MS = 15_000L
    }
}
