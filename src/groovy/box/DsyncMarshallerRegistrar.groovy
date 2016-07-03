package box

import grails.converters.JSON

class DsyncMarshallerRegistrar{

	def registerMarshallers(){
		JSON.registerObjectMarshaller(DeletedEntitySyncLog){
			prep(it, ['refId', 'syncSeq'])
		}
	}

	private prep(obj, attrs){
		def map = [:]
		if(obj){
			attrs.each { attr ->
				map[attr] = obj."$attr"
			}
		}		
		map
	}
}