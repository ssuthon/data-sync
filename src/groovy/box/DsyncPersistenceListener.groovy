package box

import org.grails.datastore.mapping.engine.event.*
import org.springframework.context.ApplicationEvent

class DsyncPersistenceListener extends AbstractPersistenceEventListener{
	def sequenceGeneratorService

	public DsyncPersistenceListener(datastore, sequenceGeneratorService) {
	    super(datastore)
	    this.sequenceGeneratorService = sequenceGeneratorService
	}

	@Override
	protected void onPersistenceEvent(final AbstractPersistenceEvent event) {
		if(event.source != this.datastore){ return; }
		if(!event.entityObject || !event.entityObject.class.isAnnotationPresent(DataSyncable))
			return	
				
		if(event instanceof PreInsertEvent || event instanceof PreUpdateEvent){
			if(event.entityObject.hasProperty('markAsDeleted') && event.entityObject.markAsDeleted){
				logDeleted(event.entityObject)
			}else{			
		  		updateSyncSeq(event.entityObject)
		  	}
		}else if(event instanceof PreDeleteEvent){
		  	logDeleted(event.entityObject)
		}
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
	    return true
	}

	private def updateSyncSeq(entityObject){
		if(entityObject.hasProperty('syncSeqAssigned'))
			return

		def nextNumber = sequenceGeneratorService.nextNumber(entityObject.class, 'dataSync') as Long
		entityObject.syncSeq = nextNumber

		entityObject.metaClass.syncSeqAssigned = true
	}

	private def logDeleted(entityObject){		
		def nextNumber = sequenceGeneratorService.nextNumber(entityObject.class, 'dataSync') as Long		
		new DeletedEntitySyncLog(store: entityObject.class.simpleName, refId: entityObject.id, syncSeq: nextNumber).save()
	}
}
