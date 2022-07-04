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
package org.hisp.dhis.artemis.audit.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory;
import org.hisp.dhis.artemis.config.UsernameSupplier;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
public class AuditHibernateListenerTest
{
    @InjectMocks
    private PostInsertAuditListener postInsertAuditListener;

    @Mock
    private AuditManager auditManager;

    @Mock
    private AuditObjectFactory objectFactory;

    @Mock
    private UsernameSupplier usernameSupplier;

    @Mock
    private SchemaService schemaService;

    @Test
    void testSavePassword()
    {
        User user = createUser( 1, "userA", "passwordA" );

        Object[] state = new Object[] { "userA", "passwordA" };
        EventSource session = mock( EventSource.class );
        EntityPersister persister = mock( EntityPersister.class );
        when( persister.getPropertyNames() ).thenReturn( new String[] { "userName", "password" } );

        Schema schema = mock( Schema.class );

        HashMap<String, Property> map = new HashMap<>();
        map.put( "userName", createProperty( "userName", true ) );
        // password property has readable = false
        map.put( "password", createProperty( "password", false ) );

        when( schema.getFieldNameMapProperties() ).thenReturn( map );
        when( schemaService.getDynamicSchema( User.class ) ).thenReturn( schema );
        Map<String, Object> auditObjectMap = (Map<String, Object>) postInsertAuditListener.createAuditEntry( user,
            state, session, 1, persister );

        // password is not included
        assertNull( auditObjectMap.get( "password" ) );
        assertEquals( 1, auditObjectMap.size() );
        assertEquals( "userA", auditObjectMap.get( "userName" ).toString() );
    }

    private User createUser( int id, String userName, String password )
    {
        User user = new User();
        user.setId( id );
        user.setUsername( userName );
        user.setPassword( password );
        return user;
    }

    private Property createProperty( String name, boolean readable )
    {
        Property property = new Property( String.class );
        property.setReadable( readable );
        property.setFieldName( name );
        property.setOwner( true );
        property.setEmbeddedObject( false );
        return property;
    }
}
