import os
import glob

# 1. Update BaseFirestoreSync.kt
path = "app/src/main/java/com/example/data/remote/BaseFirestoreSync.kt"
with open(path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("abstract fun getDao(): BaseDao<T>", """protected abstract suspend fun dbInsert(item: T)
    protected abstract suspend fun dbUpsertAll(items: List<T>)
    protected abstract suspend fun dbDelete(item: T)
    protected abstract fun dbGetAll(): Flow<List<T>>""")
content = content.replace("getDao().upsertAll(items)", "dbUpsertAll(items)")
content = content.replace("getDao().insert(item)", "dbInsert(item)")
content = content.replace("getDao().delete(item)", "dbDelete(item)")
content = content.replace("getDao().getAll()", "dbGetAll()")

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

# 2. Update all Sync classes
sync_methods = {
    "InvitationCodeSync.kt": """    override suspend fun dbInsert(item: InvitationCode) { database.invitationCodeDao().insertInvitationCode(item) }
    override suspend fun dbUpsertAll(items: List<InvitationCode>) { database.invitationCodeDao().upsertInvitationCodes(items) }
    override suspend fun dbDelete(item: InvitationCode) { database.invitationCodeDao().deleteInvitationCodeById(item.id) }
    override fun dbGetAll(): Flow<List<InvitationCode>> = database.invitationCodeDao().getAllInvitationCodes()""",
    "MemberSync.kt": """    override suspend fun dbInsert(item: MemberProfile) { database.memberDao().insertMember(item) }
    override suspend fun dbUpsertAll(items: List<MemberProfile>) { database.memberDao().upsertMembers(items) }
    override suspend fun dbDelete(item: MemberProfile) { database.memberDao().deleteMember(item) }
    override fun dbGetAll(): Flow<List<MemberProfile>> = database.memberDao().getAllMembers()""",
    "NoticeCommentSync.kt": """    override suspend fun dbInsert(item: NoticeComment) { database.noticeCommentDao().insertComment(item) }
    override suspend fun dbUpsertAll(items: List<NoticeComment>) { database.noticeCommentDao().upsertComments(items) }
    override suspend fun dbDelete(item: NoticeComment) { database.noticeCommentDao().deleteComment(item.id) }
    override fun dbGetAll(): Flow<List<NoticeComment>> = database.noticeCommentDao().getAllComments()""",
    "PublicationSync.kt": """    override suspend fun dbInsert(item: Publication) { database.publicationDao().insertPublication(item) }
    override suspend fun dbUpsertAll(items: List<Publication>) { database.publicationDao().upsertPublications(items) }
    override suspend fun dbDelete(item: Publication) { database.publicationDao().deletePublicationById(item.id) }
    override fun dbGetAll(): Flow<List<Publication>> = database.publicationDao().getAllPublications()""",
    "RideRegistrationSync.kt": """    override suspend fun dbInsert(item: RideRegistration) { database.rideDao().insertRegistration(item) }
    override suspend fun dbUpsertAll(items: List<RideRegistration>) { database.rideDao().upsertRegistrations(items) }
    override suspend fun dbDelete(item: RideRegistration) { database.rideDao().cancelRegistration(item.rideId, item.memberId) }
    override fun dbGetAll(): Flow<List<RideRegistration>> = database.rideDao().getAllRegistrations()""",
    "RideSync.kt": """    override suspend fun dbInsert(item: RideEvent) { database.rideDao().insertRide(item) }
    override suspend fun dbUpsertAll(items: List<RideEvent>) { database.rideDao().upsertRides(items) }
    override suspend fun dbDelete(item: RideEvent) { database.rideDao().updateRide(item.copy(status = "CANCELADO")) }
    override fun dbGetAll(): Flow<List<RideEvent>> = database.rideDao().getAllRides()""",
    "RoleConfigSync.kt": """    override suspend fun dbInsert(item: RoleConfig) { database.roleConfigDao().insertRoleConfig(item) }
    override suspend fun dbUpsertAll(items: List<RoleConfig>) { database.roleConfigDao().upsertRoleConfigs(items) }
    override suspend fun dbDelete(item: RoleConfig) { }
    override fun dbGetAll(): Flow<List<RoleConfig>> = database.roleConfigDao().getAllRoleConfigs()""",
}

for sync_file, methods in sync_methods.items():
    p = f"app/src/main/java/com/example/data/remote/{sync_file}"
    if os.path.exists(p):
        with open(p, "r", encoding="utf-8") as f:
            c = f.read()
        import re
        c = re.sub(r'override fun getDao\(\).*', methods, c)
        
        with open(p, "w", encoding="utf-8") as f:
            f.write(c)

# 3. Fix InvitationCodeCleanupJob.kt (Suspension functions can only be called within coroutine body)
# 'e: file:///D:/Team-Nacional-TX-Aragua/app/src/main/java/com/example/data/remote/InvitationCodeCleanupJob.kt:41:42 Suspension functions can only be called within coroutine body.'
# Line 41 is probably calling getInvitationByCode or deleteInvitationCodeById 
p = "app/src/main/java/com/example/data/remote/InvitationCodeCleanupJob.kt"
if os.path.exists(p):
    with open(p, "r", encoding="utf-8") as f:
        c = f.read()
    # just print to see it
    print("InvitationCodeCleanupJob:")
    print(c)

print("Done phase 6")
