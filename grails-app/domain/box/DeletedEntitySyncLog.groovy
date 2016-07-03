package box
class DeletedEntitySyncLog {
	String store
	Long refId
	Long syncSeq
	Date actionStamp = new Date()
}