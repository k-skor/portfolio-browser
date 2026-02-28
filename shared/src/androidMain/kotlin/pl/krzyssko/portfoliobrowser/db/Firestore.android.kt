package pl.krzyssko.portfoliobrowser.db

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import pl.krzyssko.portfoliobrowser.data.Follower
import pl.krzyssko.portfoliobrowser.data.Source
import pl.krzyssko.portfoliobrowser.db.transfer.DataSyncDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProfileDto
import pl.krzyssko.portfoliobrowser.db.transfer.ProjectDto
import pl.krzyssko.portfoliobrowser.platform.getLogging
import pl.krzyssko.portfoliobrowser.platform.isEmulator


class AndroidFirestore: Firestore {
    private val db: FirebaseFirestore

    init {
        if (isEmulator) {
            Firebase.firestore.useEmulator("10.0.2.2", 8080)
        }
        db = Firebase.firestore
    }

    override suspend fun hasUser(uid: String): Boolean {
        val result = db.collection("users").document(uid).get().await()
        return result.exists()
    }

    override suspend fun getProfile(uid: String): ProfileDto? {
        val result = db.collection("users").document(uid).get().await()
        return result?.toObject<ProfileDto>()
    }

    override suspend fun createProfile(uid: String, profile: ProfileDto) {
        db.collection("users").document(uid).set(profile, SetOptions.merge()).await()
    }

    override suspend fun createProjects(): String {
        val docRef = db.collection("projects").document()
        return docRef.id
    }

    override suspend fun syncProjects(uid: String, projectsList: List<ProjectDto>, source: Source) {
        val usersRef = db.collection("users").document(uid)
        val syncsRef = usersRef.collection("sync_data").document()
        val projectsCol = usersRef.collection("projects")

        db.runBatch { batch ->
            val dbProjects = projectsList.map {
                val projectRef = projectsCol.document()
                it.copy(id = projectRef.id).also { db ->
                    batch.set(projectRef, db)
                }
            }
            batch.set(
                syncsRef,
                DataSyncDto(
                    uid = uid,
                    timestamp = Timestamp.now()
                        .let { it.seconds * 1_000 + it.nanoseconds.toLong() / 1_000_000 },
                    source = source.toString(),
                    projectIds = dbProjects.map { it.id!! })
            )
        }.await()
    }

    suspend fun setProjectPrivateData(id: String, role: String, projectRef: DocumentReference): Task<Void> {
        return projectRef.collection("private_data").document("private").set(mapOf("role" to role), SetOptions.merge())
    }

    override suspend fun getProjects(cursor: Any?, uid: String): QueryPagedResult<ProjectDto> {
        val colRef = db.collectionGroup("projects")
        var query = colRef.where(
            Filter.or(
                Filter.equalTo("public", true),
                Filter.equalTo("createdBy", uid)
            )
        ).orderBy("followersCount", Query.Direction.DESCENDING).limit(5)

        (cursor as? DocumentSnapshot)?.let {
            query = colRef.where(
                Filter.or(
                    Filter.equalTo("public", true),
                    Filter.equalTo("createdBy", uid)
                )
            ).orderBy("followersCount", Query.Direction.DESCENDING).startAfter(it).limit(5)
        }

        val snapshot = query.get().await()
        val cursor = if (snapshot.documents.size < 5) null else snapshot.documents.lastOrNull()

        return QueryPagedResult(
            updates = (this::projectsUpdates)(query),
            value = snapshot.toObjects<ProjectDto>(),
            cursor = cursor
        )
    }

    override suspend fun getFavoriteProjects(cursor: Any?, uid: String): QueryPagedResult<ProjectDto> {
        val profile = getProfile(uid)
        val follower = Follower(
            uid = uid,
            name = profile?.alias ?: "${profile?.firstName} ${profile?.lastName}"
        )

        val colRef = db.collectionGroup("projects")
        var query = colRef.where(
            Filter.or(
                Filter.equalTo("public", true),
                Filter.equalTo("createdBy", uid)
            )
        ).whereArrayContains("followers", follower).limit(5)

        (cursor as? DocumentSnapshot)?.let {
            query = colRef.where(
                Filter.or(
                    Filter.equalTo("public", true),
                    Filter.equalTo("createdBy", uid)
                )
            ).whereArrayContains("followers", follower).startAfter(it).limit(5)
        }

        val snapshot = query.get().await()
        val cursor = if (snapshot.documents.size < 5) null else snapshot.documents.lastOrNull()

        return QueryPagedResult(
            updates = (this::projectsUpdates)(query),
            value = snapshot.toObjects<ProjectDto>(),
            cursor = cursor
        )
    }

    fun projectsUpdates(query: Query): Flow<List<ProjectDto>> = flow {
        query.snapshots().map { snapshots ->
            snapshots.toObjects<List<ProjectDto>>()
        }
    }

    override suspend fun searchProjects(phrase: String, cursor: Any?, uid: String): QueryPagedResult<ProjectDto> {
        val colRef = db.collectionGroup("projects")
        var query = colRef.where(
            Filter.or(
                Filter.equalTo("public", true),
                Filter.equalTo("createdBy", uid)
            )
        ).whereArrayContains("namePartial", phrase).limit(5)

        (cursor as? DocumentSnapshot)?.let {
            query = colRef.where(
                Filter.or(
                    Filter.equalTo("public", true),
                    Filter.equalTo("createdBy", uid)
                )
            ).whereArrayContains("namePartial", phrase).startAfter(it).limit(5)
        }

        val snapshot = query.get().await()

        return QueryPagedResult(
            updates = (this::projectsUpdates)(query),
            value = if (!snapshot.isEmpty) snapshot.toObjects<ProjectDto>() else emptyList(),
            cursor = if (!snapshot.isEmpty) snapshot.documents[snapshot.size() - 1] else null
        )
    }

    override suspend fun getProject(uid: String, ownerId: String, projectId: String): ProjectDto? {
        val collection = db.collection("users").document(ownerId).collection("projects")

        val result = if (ownerId != uid) {
            collection
                .where(
                    Filter.and(
                        Filter.equalTo("public", true),
                        Filter.equalTo("id", projectId)
                    )
                )
                .get().await()
        } else {
            collection
                .where(
                    Filter.and(
                        Filter.equalTo("createdBy", uid),
                        Filter.equalTo("id", projectId)
                    )
                )
                .get().await()
        }
        return result.documents.firstOrNull()?.toObject<ProjectDto>()
    }

    override suspend fun updateProject(uid: String, id: String?, project: ProjectDto) {
        val collection = db.collection("users").document(uid).collection("projects")
        id?.let {
            collection.document(id).set(project, SetOptions.merge()).await()
        } ?: run {
            collection.add(project).await()
        }
    }

    override suspend fun toggleFollowProject(uid: String, id: String, follower: Follower, toggle: Boolean) {
        val userRef = db.collection("users").document(uid)
        val projectRef = userRef.collection("projects").document(id)

        db.runTransaction { transition ->
            val result = transition.get(projectRef).toObject(ProjectDto::class.java)
            logging.debug("followers rd=${result?.followers?.size}")
            result?.let {
                logging.debug("putting follower=$follower")
                if (toggle) {
                    transition.update(projectRef, "followers", FieldValue.arrayUnion(follower))
                    transition.update(projectRef, "followersCount", it.followersCount + 1)
                } else {
                    transition.update(projectRef, "followers", FieldValue.arrayRemove(follower))
                    transition.update(projectRef, "followersCount", it.followersCount - 1)
                }
            } ?: throw Exception("Failed to calculate followers count")
            result
        }.await()
    }

    val logging = getLogging()

    override suspend fun getLastSyncTimestampForSource(uid: String, source: Source): Long? {
        val snapshot = db.collection("users").document(uid).collection("sync_data")
            .whereEqualTo("source", source)
            .orderBy("timestamp").limitToLast(1).get().await()

        return if (!snapshot.isEmpty) snapshot.last().toObject<DataSyncDto>().timestamp else null
    }
}

actual fun getFirestore(): Firestore = AndroidFirestore()
