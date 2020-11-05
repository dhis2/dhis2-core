package org.hisp.dhis.artemis.audit;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.artemis.audit.legacy.DefaultAuditObjectFactory;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuditObjectFactoryTest
{
    @Mock
    private DefaultAuditObjectFactory factory;

    @Before
    public void setUp()
    {
        factory = new DefaultAuditObjectFactory( new ObjectMapper() );
    }

    @Test
    public void testCollectAuditAttributes()
    {
        DataElement dataElement = new DataElement();
        dataElement.setUid( "DataElementUID" );
        dataElement.setName( "DataElementA" );

        AuditableEntity auditEntity = new AuditableEntity( DataElement.class, dataElement );
        AuditAttributes attributes = factory.collectAuditAttributes( auditEntity );
        assertNotNull( attributes );
        assertEquals( dataElement.getUid(), attributes.get( "uid" ) );

        Map<String, Object> map = new HashMap<>();
        map.put( "name", dataElement.getName() );
        map.put( "uid" , dataElement.getUid() );
        map.put( "code", "CODEA" );

        auditEntity = new AuditableEntity( DataElement.class, map );
        attributes = factory.collectAuditAttributes( auditEntity );
        assertNotNull( attributes );
        assertEquals( dataElement.getUid(), attributes.get( "uid" ) );
        assertEquals( "CODEA", attributes.get( "code" ) );
    }
}
