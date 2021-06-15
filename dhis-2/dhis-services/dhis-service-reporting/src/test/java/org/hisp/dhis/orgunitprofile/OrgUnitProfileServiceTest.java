/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.orgunitprofile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.orgunitprofile.impl.DefaultOrgUnitProfileService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.UserService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class OrgUnitProfileServiceTest
    extends DhisSpringTest
{
    @Autowired
    private OrgUnitProfileService service;

    @Autowired
    private UserService _userService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private KeyJsonValueService dataStore;

    @Mock
    private AnalyticsService mockAnalyticsService;

    @Autowired
    private ObjectMapper jsonMapper;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private OrgUnitProfileService mockService;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        createAndInjectAdminUser();
        mockService = new DefaultOrgUnitProfileService( dataStore, manager, mockAnalyticsService, organisationUnitGroupService, jsonMapper );
    }

    @Test
    public void testSave()
    {
        OrgUnitProfile orgUnitProfile = new OrgUnitProfile();
        orgUnitProfile.getAttributes().add( "Attribute1" );
        orgUnitProfile.getAttributes().add( "Attribute2" );
        orgUnitProfile.getDataItems().add( "DataItem1" );
        orgUnitProfile.getDataItems().add( "DataItem2" );
        orgUnitProfile.getGroupSets().add( "GroupSet1" );
        orgUnitProfile.getGroupSets().add( "GroupSet2" );
        service.saveOrgUnitProfile( orgUnitProfile );

        OrgUnitProfile savedProfile = service.getOrgUnitProfile();
        assertEquals( 2, savedProfile.getAttributes().size() );
        assertEquals( 2, savedProfile.getDataItems().size() );
        assertEquals( 2, savedProfile.getGroupSets().size() );
        assertTrue( savedProfile.getAttributes().contains( "Attribute1" ) );
        assertTrue( savedProfile.getDataItems().contains( "DataItem2" ) );
        assertTrue( savedProfile.getGroupSets().contains( "GroupSet1" ) );
    }

    @Test
    public void testGetProfileData()
    {
        Attribute attribute = createAttribute( 'A' );
        attribute.setOrganisationUnitAttribute( true );
        manager.save( attribute );

        OrganisationUnit orgUnit = createOrganisationUnit( "A" );
        orgUnit.getAttributeValues().add( new AttributeValue( "testAttributeValue", attribute) );
        manager.save( orgUnit );

        OrganisationUnitGroup group = createOrganisationUnitGroup( 'A' );
        group.addOrganisationUnit( orgUnit );
        manager.save( group );

        OrganisationUnitGroupSet groupSet = createOrganisationUnitGroupSet( 'A' );
        groupSet.addOrganisationUnitGroup( group );
        manager.save( groupSet );

        DataElement dataElement = createDataElement( 'A' );
        manager.save( dataElement );

        Period period = createPeriod( "202106" );
        manager.save( period );

        OrgUnitProfile orgUnitProfile = new OrgUnitProfile();
        orgUnitProfile.getAttributes().add( attribute.getUid() );
        orgUnitProfile.getDataItems().add( dataElement.getUid() );
        orgUnitProfile.getGroupSets().add( groupSet.getUid() );
        service.saveOrgUnitProfile( orgUnitProfile );

        //Mock analytic query for data value
        Map<String,Object> mapDataItem = new HashMap<>();
        mapDataItem.put( dataElement.getUid(), "testDataValue" );
        Mockito.when( mockAnalyticsService.getAggregatedDataValueMapping( any( DataQueryParams.class ) ) )
            .thenReturn( mapDataItem );

        OrgUnitProfileData data = mockService.getOrgUnitProfileData( orgUnit.getUid(), period.getIsoDate() );

        assertEquals( "testAttributeValue", data.getAttributes().get( 0 ).getValue() );
        assertEquals( "testDataValue", data.getDataItems().get( 0 ).getValue() );
        assertEquals( group.getDisplayName(), data.getGroupSets().get( 0 ).getValue() );
    }
}
