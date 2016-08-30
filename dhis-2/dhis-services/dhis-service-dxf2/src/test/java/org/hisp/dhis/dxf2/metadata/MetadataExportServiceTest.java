package org.hisp.dhis.dxf2.metadata;

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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dxf2.metadata.MetadataExportParams;
import org.hisp.dhis.dxf2.metadata.MetadataExportService;
import org.hisp.dhis.query.Disjunction;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class MetadataExportServiceTest
    extends DhisSpringTest
{
    @Autowired
    private MetadataExportService metadataExportService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private SchemaService schemaService;

    @Test
    public void testValidate()
    {
        MetadataExportParams params = new MetadataExportParams();
        metadataExportService.validate( params );
    }

    @Test
    public void testMetadataExport()
    {
        DataElementGroup deg1 = createDataElementGroup( 'A' );
        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        User user = createUser( 'A' );
        manager.save( user );

        deg1.addDataElement( de1 );
        deg1.addDataElement( de2 );
        deg1.addDataElement( de3 );

        deg1.setUser( user );
        manager.save( deg1 );

        MetadataExportParams params = new MetadataExportParams();
        Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata = metadataExportService.getMetadata( params );

        assertEquals( 1, metadata.get( User.class ).size() );
        assertEquals( 1, metadata.get( DataElementGroup.class ).size() );
        assertEquals( 3, metadata.get( DataElement.class ).size() );
    }

    @Test
    public void testMetadataExportWithCustomClasses()
    {
        DataElementGroup deg1 = createDataElementGroup( 'A' );
        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        User user = createUser( 'A' );
        manager.save( user );

        deg1.addDataElement( de1 );
        deg1.addDataElement( de2 );
        deg1.addDataElement( de3 );

        deg1.setUser( user );
        manager.save( deg1 );

        MetadataExportParams params = new MetadataExportParams();
        params.addClass( DataElement.class );

        Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata = metadataExportService.getMetadata( params );

        assertFalse( metadata.containsKey( User.class ) );
        assertFalse( metadata.containsKey( DataElementGroup.class ) );
        assertTrue( metadata.containsKey( DataElement.class ) );

        assertEquals( 3, metadata.get( DataElement.class ).size() );
    }

    @Test
    public void testMetadataExportWithCustomQueries()
    {
        DataElementGroup deg1 = createDataElementGroup( 'A' );
        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        User user = createUser( 'A' );
        manager.save( user );

        deg1.addDataElement( de1 );
        deg1.addDataElement( de2 );
        deg1.addDataElement( de3 );

        deg1.setUser( user );
        manager.save( deg1 );

        Query deQuery = Query.from( schemaService.getDynamicSchema( DataElement.class ) );

        Disjunction disjunction = deQuery.disjunction();
        disjunction.add( Restrictions.eq( "id", de1.getUid() ) );
        disjunction.add( Restrictions.eq( "id", de2.getUid() ) );

        deQuery.add( disjunction );

        Query degQuery = Query.from( schemaService.getDynamicSchema( DataElementGroup.class ) );
        degQuery.add( Restrictions.eq( "id", "INVALID UID" ) );

        MetadataExportParams params = new MetadataExportParams();
        params.addQuery( deQuery );
        params.addQuery( degQuery );

        Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> metadata = metadataExportService.getMetadata( params );

        assertFalse( metadata.containsKey( User.class ) );
        assertFalse( metadata.containsKey( DataElementGroup.class ) );
        assertTrue( metadata.containsKey( DataElement.class ) );

        assertEquals( 2, metadata.get( DataElement.class ).size() );
    }
}
