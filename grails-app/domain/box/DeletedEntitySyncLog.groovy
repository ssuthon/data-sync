package box
class DeletedEntitySyncLog {
	String store
	Long refId
	Long syncSeq
	Date actionStamp = new Date()


	static mapping = {
		store index: 'DES_STORE_IDX'
		syncSeq index: 'DES_SYNCSEQ_IDX'
	}
}