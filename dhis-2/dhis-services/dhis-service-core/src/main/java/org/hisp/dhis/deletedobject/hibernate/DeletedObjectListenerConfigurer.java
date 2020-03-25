package org.hisp.dhis.deletedobject.hibernate;

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

@Component
public class DeletedObjectListenerConfigurer
{
    @PersistenceUnit
    private EntityManagerFactory emf;

    private final DeletedObjectPostInsertEventListener insertEventListener;

    private final DeletedObjectPostDeleteEventListener deleteEventListener;

    public DeletedObjectListenerConfigurer( DeletedObjectPostInsertEventListener insertEventListener,
        DeletedObjectPostDeleteEventListener deleteEventListener )
    {
        this.deleteEventListener = deleteEventListener;
        this.insertEventListener = insertEventListener;
    }

    @PostConstruct
    protected void init()
    {
        SessionFactoryImpl sessionFactory = emf.unwrap( SessionFactoryImpl.class );

        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService( EventListenerRegistry.class );

        registry.getEventListenerGroup( EventType.POST_COMMIT_INSERT ).appendListener( insertEventListener );

        registry.getEventListenerGroup( EventType.POST_DELETE ).appendListener( deleteEventListener );
    }
}
