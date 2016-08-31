package org.hisp.dhis.hibernate;

/*
 * Copyright (c) 2004-2016, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.SessionFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateEventListenerWiring
{
    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IdentifiableObjectManager objectManager;

    private Set<IdentifiableObject> identifiableObjects = new HashSet<>();

    @PostConstruct
    public void registerListeners()
    {
        EventListenerRegistry registry = ((SessionFactoryImpl) sessionFactory).getServiceRegistry()
            .getService( EventListenerRegistry.class );

        registry.getEventListenerGroup( EventType.PRE_COLLECTION_UPDATE ).appendListener( preCollectionUpdateEventListener );
        registry.getEventListenerGroup( EventType.POST_COMMIT_UPDATE ).appendListener( postUpdateEventListener );
        registry.getEventListenerGroup( EventType.POST_COMMIT_INSERT ).appendListener( postInsertEventListener );
        registry.getEventListenerGroup( EventType.POST_COMMIT_DELETE ).appendListener( postDeleteEventListener );
    }

    @SuppressWarnings( "unchecked" )
    private PreCollectionUpdateEventListener preCollectionUpdateEventListener = new PreCollectionUpdateEventListener()
    {
        @Override
        public void onPreUpdateCollection( PreCollectionUpdateEvent event )
        {
            if ( event.getAffectedOwnerOrNull() != null )
            {
                if ( event.getAffectedOwnerOrNull() instanceof IdentifiableObject )
                {
                    identifiableObjects.add( (IdentifiableObject) event.getAffectedOwnerOrNull() );
                }
            }

            Object newValue = event.getCollection().getValue();
            Serializable oldValue = event.getCollection().getStoredSnapshot();

            Collection<Object> newCol = new ArrayList<>();
            Collection<Object> oldCol = new ArrayList<>();

            if ( Collection.class.isInstance( newValue ) )
            {
                newCol = new ArrayList<>( (Collection<Object>) newValue );

                if ( !newCol.isEmpty() )
                {
                    Object next = newCol.iterator().next();

                    if ( !(next instanceof IdentifiableObject) )
                    {
                        newCol = new ArrayList<>();
                    }
                }
            }

            Map<?, ?> map = (Map<?, ?>) oldValue;

            if ( oldValue != null )
            {
                for ( Object o : map.keySet() )
                {
                    if ( o instanceof IdentifiableObject )
                    {
                        oldCol.add( o );
                    }
                }
            }

            Collection<? extends IdentifiableObject> removed = CollectionUtils.subtract( oldCol, newCol );
            Collection<? extends IdentifiableObject> added = CollectionUtils.subtract( newCol, oldCol );

            identifiableObjects.addAll( removed );
            identifiableObjects.addAll( added );
        }
    };

    private PostUpdateEventListener postUpdateEventListener = new PostUpdateEventListener()
    {
        @Override
        public void onPostUpdate( PostUpdateEvent event )
        {
            updateIdentifiableObjects();
        }
    };

    private PostInsertEventListener postInsertEventListener = new PostInsertEventListener()
    {
        @Override
        public void onPostInsert( PostInsertEvent event )
        {
            updateIdentifiableObjects();
        }
    };

    private PostDeleteEventListener postDeleteEventListener = new PostDeleteEventListener()
    {
        @Override
        public void onPostDelete( PostDeleteEvent event )
        {
            updateIdentifiableObjects();
        }
    };

    private void updateIdentifiableObjects()
    {
        if ( identifiableObjects.isEmpty() )
        {
            return;
        }

        objectManager.update( new ArrayList<>( identifiableObjects ) );
        identifiableObjects.clear();
    }
}
