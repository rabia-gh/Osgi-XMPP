package org.supcom.event;

import java.security.Permission;
import java.util.Map;
import java.util.Set;
import org.eclipse.osgi.framework.eventmgr.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.*;
import org.osgi.service.log.LogService;


public class EventAdminImpl implements EventAdmin {
	private final LogTracker log;
	private final EventHandlerTracker handlers;
	private volatile EventManager eventManager;

	 
	@param context BundleContext
	EventAdminImpl(BundleContext context) {
		super();
		log = new LogTracker(context, System.out);
		handlers = new EventHandlerTracker(context, log);
	}

	/**
	 * This method should be called before registering EventAdmin service
	 */
	void start() {
		log.open();
		ThreadGroup eventGroup = new ThreadGroup("Equinox Event Admin"); //$NON-NLS-1$
		eventGroup.setDaemon(true);
		eventManager = new EventManager(EventAdminMsg.EVENT_ASYNC_THREAD_NAME, eventGroup);
		handlers.open();
	}

	
	void stop() {
		handlers.close();
		eventManager.close();
		eventManager = null; // signify we have stopped
		log.close();
	}

	@Override
	public void postEvent(Event event) {
		dispatchEvent(event, true);
	}

	@Override
	public void sendEvent(Event event) {
		dispatchEvent(event, false);
	}

	 
	@param event
	@param isAsync 
	 
	private void dispatchEvent(Event event, boolean isAsync) {
		// keep a local copy in case we are stopped in the middle of dispatching
		EventManager currentManager = eventManager;
		if (currentManager == null) {
			// EventAdmin is stopped
			return;
		}
		if (event == null) {
			log.log(LogService.LOG_ERROR, EventAdminMsg.EVENT_NULL_EVENT);
			// continue from here will result in an NPE below; the spec for EventAdmin does not allow for null here
		}

		String topic = event.getTopic();

		try {
			checkTopicPermissionPublish(topic);
		} catch (SecurityException e) {
			String msg = NLS.bind(EventAdminMsg.EVENT_NO_TOPICPERMISSION_PUBLISH, event.getTopic());
			log.log(LogService.LOG_ERROR, msg);
			// must throw a security exception here according to the EventAdmin spec
			throw e;
		}

		Set<EventHandlerWrapper> eventHandlers = handlers.getHandlers(topic);
		// If there are no handlers, then we are done
		if (eventHandlers.isEmpty()) {
			return;
		}

		SecurityManager sm = System.getSecurityManager();
		Permission perm = (sm == null) ? null : new TopicPermission(topic, TopicPermission.SUBSCRIBE);

		Map<EventHandlerWrapper, Permission> listeners = new CopyOnWriteIdentityMap<>();
		for (EventHandlerWrapper wrapper : eventHandlers)
			listeners.put(wrapper, perm);

		// Create the listener queue for this event delivery
		ListenerQueue<EventHandlerWrapper, Permission, Event> listenerQueue = new ListenerQueue<>(currentManager);
		// Add the listeners to the queue and associate them with the event
		// dispatcher
		listenerQueue.queueListeners(listeners.entrySet(), handlers);
		// Deliver the event to the listeners.
		if (isAsync) {
			listenerQueue.dispatchEventAsynchronous(0, event);
		} else {
			listenerQueue.dispatchEventSynchronous(0, event);
		}
	}

	/**
	 * Checks if the caller bundle has right PUBLISH TopicPermision.
	 * 
	 * @param topic
	 * @throws SecurityException if the caller does not have the right to PUBLISH TopicPermission
	 */
	private void checkTopicPermissionPublish(String topic) throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null)
			return;
		sm.checkPermission(new TopicPermission(topic, TopicPermission.PUBLISH));
	}

}