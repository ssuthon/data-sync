package box
import grails.converters.JSON

class DsyncController {
	def grailsApplication

	def forUpdated(String store, Long lastSyncSeq){
		def dc = findDomainClassByName(store.capitalize())
		if(!dc){
			response.sendError(422)
			return
		}

		lastSyncSeq = lastSyncSeq ?: 0
		def result = dc.createCriteria().list {
			gt 'syncSeq', lastSyncSeq
			order 'syncSeq', 'asc'
			maxResults 100
		}

		render result as JSON
	}

	def forDeleted(String store, Long lastSyncSeq){
		def dc = findDomainClassByName(store.capitalize())
		if(!dc){
			response.sendError(422)
			return
		}

		lastSyncSeq = lastSyncSeq ?: 0
		def result = DeletedEntitySyncLog.createCriteria().list {
			eq 'store', store
			gt 'syncSeq', lastSyncSeq
			order 'syncSeq', 'asc'
			maxResults 100
		}

		render result as JSON
	}

	def upload(String store){
		def dc = findDomainClassByName(store.capitalize())
    	if(!dc || !dc.isAnnotationPresent(DataUploadable)){
			response.sendError(422)
			return
		}
		def instance = dc.newInstance()
		def jsonData = request.JSON
		def result = [valid: false]
		try{
			bindData(instance, jsonData)
			instance.save(flush: true)
			if(instance.hasErrors())
				log.debug instance.errors
			result.valid = (instance.id != null)
		}catch(e){}
		render result as JSON
	}

	private def findDomainClassByName(name){
		grailsApplication.domainClasses.find { it.clazz.simpleName == name }?.clazz
	}
}