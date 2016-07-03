import box.*

class DataSyncBootStrap {		
    def grailsApplication
	def sequenceGeneratorService
    def dsyncMarshallerRegistrar

    def init = { servletContext ->
        dsyncMarshallerRegistrar.registerMarshallers()
        
        println "initialize sequence generator"
    	grailsApplication.domainClasses.each { domain ->
            if(!domain.clazz.isAnnotationPresent(DataSyncable))
                return

            def v1 = domain.clazz.createCriteria().get {
                projections {
                    max "syncSeq"
                }
            } as Long ?: 1

            def v2 = DeletedEntitySyncLog.createCriteria().get {
                eq 'store', domain.clazz.simpleName
                projections {
                    max "syncSeq"
                }
            } as Long ?: 1

            println "init store: $domain.clazz.simpleName with ${Math.max(v1, v2)}"
            sequenceGeneratorService.initSequence(domain.clazz, 'dataSync', Math.max(v1, v2))
        }
    }
    def destroy = {
    }
}
