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
			maxResults grailsApplication.config.box.dsync.maxUpdatePerRound ?: 500
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
			eq 'store', store.capitalize()
			gt 'syncSeq', lastSyncSeq
			order 'syncSeq', 'asc'
			maxResults grailsApplication.config.box.dsync.maxDeletePerRound ?: 100
		}

		render result as JSON
	}

	def upload(String store){
		def data = request.JSON ?: JSON.parse(params.jsonData)
		log.debug "processing ${data}"

		if(data.__store){
			store = data.__store
		}

		def dc = findDomainClassByName(toCamelCase(store, true))
    	if(!dc || !dc.isAnnotationPresent(DataUploadable)){
			response.sendError(422)
			return
		}
		
		def result = updateData(dc, data)		
		render result as JSON
	}

	private updateData(dc, data, fileSupport = true){
		def instance = null
		if(data.remoteId){
			instance = dc.get(data.remoteId)
		}else if(data.uuid){
			instance = dc.findBySyncUuid(data.uuid)
		}
		
		if(!instance)
			instance = dc.newInstance()		
		
		def result = [valid: false]
		try{
			bindData(instance, data)
			if(data.__id){
				instance.id = data.__id;
			}
			if(data.uuid){
				instance.uuid = data.uuid
			}
			if(request.fileNames){
				request.fileNames.each{ file ->
					instance[file] = request.getFile(file)?.bytes
				}
			}
			instance.save flush: true, failOnError: true
			result.valid = (instance.id != null)
			result.remoteId = instance.id	
		}catch(e){
			log.debug('fail to save upload data: ' + e)
		}
		result
	}

	//do not support file update
	def uploadMultiple(String store){
		def data = request.JSON ?: JSON.parse(params.jsonData)
		log.debug "processing ${data}"

		if(data.__store){
			store = data.__store
		}

		def dc = findDomainClassByName(toCamelCase(store, true))
    	if(!dc || !dc.isAnnotationPresent(DataUploadable)){
			response.sendError(422)
			return
		}

		def results = []
		data.items.each {
			results.add(updateData(dc, it, false))
		}		
		render results as JSON
	}

	private def findDomainClassByName(name){
		grailsApplication.domainClasses.find { it.clazz.simpleName == name }?.clazz
	}

	private static String toCamelCase( String text, boolean capitalized = false ) {
        text = text.replaceAll( "(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
        return capitalized ? text.capitalize() : text
    }
}