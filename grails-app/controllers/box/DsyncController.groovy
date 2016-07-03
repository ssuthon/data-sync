package box
import grails.converters.JSON

class DsyncController {
	def grailsApplication

	def forUpdated(String store, Long lastSyncSeq){
		def dc = findDomainClassByName(store)
		if(!dc){
			response.sendError(422)
			return
		}

		lastSyncSeq = lastSyncSeq ?: 0
		def result = dc.createCriteria().list {
			gt 'syncSeq', lastSyncSeq
		}

		render result as JSON
	}

	def forDeleted(String store, Long lastSyncSeq){
		def dc = findDomainClassByName(store)
		if(!dc){
			response.sendError(422)
			return
		}

		lastSyncSeq = lastSyncSeq ?: 0
		def result = DeletedEntitySyncLog.createCriteria().list {
			eq 'store', store
			gt 'syncSeq', lastSyncSeq
		}

		render result as JSON
	}

	private def findDomainClassByName(name){
		grailsApplication.domainClasses.find { it.clazz.simpleName == name }?.clazz
	}
}