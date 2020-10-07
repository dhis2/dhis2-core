package org.hisp.dhis.artemis.audit;

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
