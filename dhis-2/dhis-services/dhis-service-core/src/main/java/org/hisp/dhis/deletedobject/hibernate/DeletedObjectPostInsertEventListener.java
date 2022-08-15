/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.deletedobject.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.StatelessSession;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.cacheinvalidation.debezium.KnownTransactionsService;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.deletedobject.DeletedObject;
import org.hisp.dhis.deletedobject.DeletedObjectQuery;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeletedObjectPostInsertEventListener
    implements PostCommitInsertEventListener
{
    private final DeletedObjectService deletedObjectService;

    private final transient KnownTransactionsService knownTransactionsService;

    public DeletedObjectPostInsertEventListener( DeletedObjectService deletedObjectService,
        KnownTransactionsService knownTransactionsService )
    {
        checkNotNull( deletedObjectService );
        checkNotNull( knownTransactionsService );
        this.deletedObjectService = deletedObjectService;
        this.knownTransactionsService = knownTransactionsService;
    }

    @Override
    public boolean requiresPostCommitHanding( EntityPersister persister )
    {
        return true;
    }

    @Override
    public void onPostInsert( PostInsertEvent event )
    {
        if ( IdentifiableObject.class.isInstance( event.getEntity() )
            && MetadataObject.class.isInstance( event.getEntity() )
            && !EmbeddedObject.class.isInstance( event.getEntity() ) )
        {
            StatelessSession session = event.getPersister().getFactory().openStatelessSession();
            session.beginTransaction();

            try
            {
                List<DeletedObject> deletedObjects = deletedObjectService
                    .getDeletedObjects( new DeletedObjectQuery( (IdentifiableObject) event.getEntity() ) );

                deletedObjects.forEach( session::delete );

                knownTransactionsService.registerEvent( event );

                session.getTransaction().commit();
            }
            catch ( Exception ex )
            {
                log.error( "Failed to delete DeletedObject for:" + event.getEntity() );
                session.getTransaction().rollback();
            }
            finally
            {
                session.close();
            }
        }
    }

    @Override
    public void onPostInsertCommitFailed( PostInsertEvent event )
    {
        log.debug( "onPostInsertCommitFailed: " + event );
    }
}
