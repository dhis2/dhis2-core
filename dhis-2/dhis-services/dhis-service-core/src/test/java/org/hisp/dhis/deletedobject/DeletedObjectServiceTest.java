package org.hisp.dhis.deletedobject;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static org.hamcrest.CoreMatchers.*;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DeletedObjectServiceTest
    extends DhisSpringTest
{
    @Autowired
    private DeletedObjectService deletedObjectService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private DataElementStore dataElementStore;

    @Autowired
    private OrganisationUnitStore organisationUnitStore;

    private DeletedObject elementA = new DeletedObject( createDataElement( 'A' ) );

    private DeletedObject elementB = new DeletedObject( createDataElement( 'B' ) );

    private DeletedObject elementC = new DeletedObject( createDataElement( 'C' ) );

    private DeletedObject elementD = new DeletedObject( createDataElement( 'D' ) );

    private DeletedObject elementE = new DeletedObject( createDataElement( 'E' ) );

    @Override
    protected void setUpTest()
        throws Exception
    {

    }

    @Override
    protected void tearDownTest()
    {
        deletedObjectService.deleteDeletedObjects( DeletedObjectQuery.EMPTY );
    }

    @Test
    public void testAddDeletedObject()
    {
        deletedObjectService.addDeletedObject( elementA );
        deletedObjectService.addDeletedObject( elementB );
        deletedObjectService.addDeletedObject( elementC );
        assertEquals( 3, deletedObjectService.countDeletedObjects() );
    }

    @Test
    public void testGetDeletedObject()
    {
        deletedObjectService.addDeletedObject( elementA );
        deletedObjectService.addDeletedObject( elementB );
        deletedObjectService.addDeletedObject( elementC );
        deletedObjectService.addDeletedObject( elementD );
        deletedObjectService.addDeletedObject( elementE );
        DeletedObjectQuery deletedObjectQuery = new DeletedObjectQuery();
        deletedObjectQuery.setTotal( 5 );
        deletedObjectQuery.setPageSize( 2 );

        deletedObjectQuery.setPage( 1 );
        List<DeletedObject> firstPageDeletedObjects = deletedObjectService.getDeletedObjects( deletedObjectQuery );
        deletedObjectQuery.setPage( 2 );
        List<DeletedObject> secondPageDeletedObjects = deletedObjectService.getDeletedObjects( deletedObjectQuery );
        deletedObjectQuery.setPage( 3 );
        List<DeletedObject> thirdPageDeletedObjects = deletedObjectService.getDeletedObjects( deletedObjectQuery );

        assertEquals( 5, deletedObjectService.countDeletedObjects() );
        assertEquals( 2, firstPageDeletedObjects.size() );
        assertEquals( 2, secondPageDeletedObjects.size() );
        assertEquals( 1, thirdPageDeletedObjects.size() );
        assertThat( firstPageDeletedObjects, hasItems( elementA, elementB ) );
        assertThat( secondPageDeletedObjects, hasItems( elementC, elementD ) );
        assertThat( thirdPageDeletedObjects, hasItems( elementE ) );
    }

    @Test
    public void testSearchForKlass()
    {
        deletedObjectService.addDeletedObject( elementA );
        deletedObjectService.addDeletedObject( elementB );
        deletedObjectService.addDeletedObject( new DeletedObject( createOrganisationUnit( 'A' ) ) );
        deletedObjectService.addDeletedObject( new DeletedObject( createOrganisationUnit( 'B' ) ) );
        deletedObjectService.addDeletedObject( new DeletedObject( createOrganisationUnit( 'C' ) ) );

        assertEquals( 5, deletedObjectService.countDeletedObjects() );
        assertEquals( 2, deletedObjectService.getDeletedObjectsByKlass( "DataElement" ).size() );
        assertEquals( 3, deletedObjectService.getDeletedObjectsByKlass( "OrganisationUnit" ).size() );
        assertTrue( deletedObjectService.getDeletedObjectsByKlass( "Indicator" ).isEmpty() );
    }

    @Test
    public void testDeleteDataElement()
    {
        DataElement dataElementF = createDataElement( 'F' );
        DataElement dataElementG = createDataElement( 'G' );
        DataElement dataElementH = createDataElement( 'H' );
        OrganisationUnit organisationUnitX = createOrganisationUnit( 'X' );
        OrganisationUnit organisationUnitY = createOrganisationUnit( 'Y' );

        dataElementStore.save( dataElementF );
        dataElementStore.save( dataElementG );
        dataElementStore.save( dataElementH );
        organisationUnitStore.save( organisationUnitX );
        organisationUnitStore.save( organisationUnitY );

        dataElementStore.delete( dataElementF );
        dataElementStore.delete( dataElementG );
        dataElementStore.delete( dataElementH );
        organisationUnitStore.delete( organisationUnitX );
        organisationUnitStore.delete( organisationUnitY );

        manager.flush();

        assertEquals( 5, deletedObjectService.countDeletedObjects() );
        assertEquals( 3, deletedObjectService.getDeletedObjectsByKlass( "DataElement" ).size() );
        assertEquals( 2, deletedObjectService.getDeletedObjectsByKlass( "OrganisationUnit" ).size() );
    }
}
