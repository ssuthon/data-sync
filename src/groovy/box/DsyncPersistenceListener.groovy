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

	    /*switch(event.eventType) {	  
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
	    }*/
if(event instanceof PreInsertEvent || event instanceof PreUpdateEvent){
   println 'before inserting... or updating...'
}else if(event instanceof PreDeleteEvent){
  logDeleted(event.entityObject)
}
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
	    return true
	}

	private def updateSyncSeq(entityObject){
	
		def nextNumber = sequenceGeneratorService.nextNumber(entityObject.class, 'dataSync') as Long
		entityObject.syncSeq = nextNumber
	}

	private def logDeleted(entityObject){		
		def nextNumber = sequenceGeneratorService.nextNumber(entityObject.class, 'dataSync') as Long		
		new DeletedEntitySyncLog(store: entityObject.class.simpleName, refId: entityObject.id, syncSeq: nextNumber).save()
	}
}
