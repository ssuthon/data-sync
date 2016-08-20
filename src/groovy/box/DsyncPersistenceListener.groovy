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
		if(!event.entityObject?.class.isAnnotationPresent(DataSyncable))
			return		

	    switch(event.eventType) {	  
	    	case EventType.Validation:
	    		if(event.entityObject.syncSeq == null){
	    			event.entityObject.syncSeq = 0
	    		}
	    		break      	       
	        case EventType.SaveOrUpdate:
	            updateSyncSeq(event.entityObject)
	        	break
	        
	        case EventType.PreDelete:
	            logDeleted(event.entityObject)
	        	break
	    }
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
	    return true
	}

	private def updateSyncSeq(entityObject){
	
		//if(entityObject.hasProperty('syncSeqAssigned'))
		//	return

		def nextNumber = sequenceGeneratorService.nextNumber(entityObject.class, 'dataSync') as Long
		entityObject.syncSeq = nextNumber
		//entityObject.metaClass.syncSeqAssigned = true
	}

	private def logDeleted(entityObject){		
		def nextNumber = sequenceGeneratorService.nextNumber(entityObject.class, 'dataSync') as Long		
		new DeletedEntitySyncLog(store: entityObject.class.simpleName, refId: entityObject.id, syncSeq: nextNumber).save()
	}
}