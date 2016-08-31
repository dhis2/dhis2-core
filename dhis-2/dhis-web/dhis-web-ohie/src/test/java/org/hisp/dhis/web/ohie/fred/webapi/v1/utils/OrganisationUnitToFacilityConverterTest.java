package org.hisp.dhis.web.ohie.fred.webapi.v1.utils;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.web.ohie.fred.webapi.v1.domain.Facility;
import org.hisp.dhis.web.ohie.fred.webapi.v1.domain.Identifier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class OrganisationUnitToFacilityConverterTest
{
    private OrganisationUnitToFacilityConverter converter;

    @Before
    public void setup()
    {
        converter = new OrganisationUnitToFacilityConverter();
    }

    @Test
    public void testName()
    {
        OrganisationUnit organisationUnit = new OrganisationUnit( "OrgUnit1" );
        Facility facility = converter.convert( organisationUnit );

        Assert.assertEquals( facility.getName(), organisationUnit.getName() );
    }

    @Test
    public void testParent()
    {
        OrganisationUnit organisationUnit = new OrganisationUnit( "OrgUnit1" );
        OrganisationUnit parent = new OrganisationUnit( "parent" );
        organisationUnit.setParent( parent );

        organisationUnit.setAutoFields();
        parent.setAutoFields();

        Facility facility = converter.convert( organisationUnit );

        String parent1 = (String) facility.getProperties().get( "parent" );
        Assert.assertEquals( parent.getUid(), parent1 );
    }

    @Test
    public void testIdentifiers()
    {
        OrganisationUnit organisationUnit = new OrganisationUnit( "OrgUnit1" );
        organisationUnit.setAutoFields();
        organisationUnit.setCode( "code" );

        Facility facility = converter.convert( organisationUnit );

        boolean foundUid = false;
        boolean foundCode = false;

        for ( Identifier identifier : facility.getIdentifiers() )
        {
            if ( identifier.getAgency().equalsIgnoreCase( Identifier.DHIS2_AGENCY ) )
            {
                if ( identifier.getContext().equalsIgnoreCase( Identifier.DHIS2_UID_CONTEXT ) )
                {
                    Assert.assertEquals( identifier.getId(), organisationUnit.getUid() );
                    foundUid = true;
                }
                else if ( identifier.getContext().equalsIgnoreCase( Identifier.DHIS2_CODE_CONTEXT ) )
                {
                    Assert.assertEquals( identifier.getId(), organisationUnit.getCode() );
                    foundCode = true;
                }
            }
        }

        Assert.assertTrue( foundUid );
        Assert.assertTrue( foundCode );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDataSets()
    {
        OrganisationUnit organisationUnit = new OrganisationUnit( "OrgUnit1" );
        DataSet dataSet1 = new DataSet( "dataSet1" );
        DataSet dataSet2 = new DataSet( "dataSet1" );
        DataSet dataSet3 = new DataSet( "dataSet1" );

        dataSet1.setAutoFields();
        dataSet2.setAutoFields();
        dataSet3.setAutoFields();

        organisationUnit.getDataSets().add( dataSet1 );
        organisationUnit.getDataSets().add( dataSet2 );
        organisationUnit.getDataSets().add( dataSet3 );

        Facility facility = converter.convert( organisationUnit );

        List<String> dataSets = (List<String>) facility.getProperties().get( "dataSets" );

        Assert.assertEquals( 3, dataSets.size() );
        Assert.assertTrue( dataSets.contains( dataSet1.getUid() ) );
        Assert.assertTrue( dataSets.contains( dataSet2.getUid() ) );
        Assert.assertTrue( dataSets.contains( dataSet3.getUid() ) );
    }
}
