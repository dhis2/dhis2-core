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
package org.hisp.dhis.orgunitprofile.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.keyjsonvalue.KeyJsonNamespaceProtection;
import org.hisp.dhis.keyjsonvalue.KeyJsonValue;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.orgunitprofile.OrgUnitInfo;
import org.hisp.dhis.orgunitprofile.OrgUnitProfile;
import org.hisp.dhis.orgunitprofile.OrgUnitProfileData;
import org.hisp.dhis.orgunitprofile.OrgUnitProfileService;
import org.hisp.dhis.orgunitprofile.ProfileItem;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.program.ProgramIndicator;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

@Slf4j
@Service
public class DefaultOrgUnitProfileService
    implements OrgUnitProfileService
{
    private static final List<Class<? extends IdentifiableObject>> DATA_ITEM_CLASSES = ImmutableList
        .<Class<? extends IdentifiableObject>> builder()
        .add( DataElement.class ).add( Indicator.class ).add( DataSet.class ).add( ProgramIndicator.class )
        .build();

    private KeyJsonValueService dataStore;

    private IdentifiableObjectManager idObjectManager;

    private AnalyticsService analyticsService;

    private OrganisationUnitGroupService groupService;

    private ObjectMapper jsonMapper;

    public DefaultOrgUnitProfileService( KeyJsonValueService dataStore,
        IdentifiableObjectManager idObjectManager, AnalyticsService analyticsService,
        OrganisationUnitGroupService groupService, ObjectMapper jsonMapper )
    {
        this.dataStore = dataStore;
        this.idObjectManager = idObjectManager;
        this.analyticsService = analyticsService;
        this.jsonMapper = jsonMapper;
        this.groupService = groupService;

        this.dataStore.addProtection(
            new KeyJsonNamespaceProtection( ORG_UNIT_PROFILE_NAMESPACE, KeyJsonNamespaceProtection.ProtectionType.NONE,
                KeyJsonNamespaceProtection.ProtectionType.RESTRICTED, false, "ALL", ORG_UNIT_PROFILE_AUTHORITY ) );
    }

    @Override
    @Transactional
    public void saveOrgUnitProfile( OrgUnitProfile profile )
    {
        KeyJsonValue keyJsonValue = new KeyJsonValue( ORG_UNIT_PROFILE_KEY, ORG_UNIT_PROFILE_NAMESPACE );

        try
        {
            keyJsonValue.setValue( jsonMapper.writeValueAsString( profile ) );
            dataStore.saveOrUpdateKeyJsonValue( keyJsonValue );
        }
        catch ( JsonProcessingException e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
            throw new IllegalArgumentException( "Can't serialize OrgUnitProfile " + profile );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public OrgUnitProfile getOrgUnitProfile()
    {
        KeyJsonValue value = dataStore.getKeyJsonValue( ORG_UNIT_PROFILE_NAMESPACE, ORG_UNIT_PROFILE_KEY );

        if ( value == null )
        {
            return new OrgUnitProfile();
        }

        try
        {
            return jsonMapper.readValue( value.getValue(), OrgUnitProfile.class );
        }
        catch ( JsonProcessingException e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
            throw new IllegalArgumentException( "Can't deserialize OrgUnitProfile " + value.getValue() );
        }
    }

    @Override
    public OrgUnitProfileData getOrgUnitProfileData( String orgUnit, @Nullable String isoPeriod )
    {
        // If profile is empty, only fixed info will be included

        OrgUnitProfile profile = getOrgUnitProfile();

        OrganisationUnit unit = getOrgUnit( orgUnit );

        Period period = getPeriod( isoPeriod );

        OrgUnitProfileData data = new OrgUnitProfileData();

        data.setInfo( getOrgUnitInfo( unit ) );
        data.setAttributes( getAttributes( profile, unit ) );
        data.setGroupSets( getGroupSets( profile, unit ) );
        data.setDataItems( getDataItems( profile, unit, period ) );

        return data;
    }

    private OrgUnitInfo getOrgUnitInfo( OrganisationUnit orgUnit )
    {
        OrgUnitInfo info = new OrgUnitInfo();

        info.setId( orgUnit.getUid() );
        info.setCode( orgUnit.getCode() );
        info.setName( orgUnit.getDisplayName() );
        info.setShortName( orgUnit.getDisplayShortName() );
        info.setDescription( orgUnit.getDisplayDescription() );
        info.setOpeningDate( orgUnit.getOpeningDate() );
        info.setClosedDate( orgUnit.getClosedDate() );
        info.setComment( orgUnit.getComment() );
        info.setUrl( orgUnit.getUrl() );
        info.setContactPerson( orgUnit.getContactPerson() );
        info.setAddress( orgUnit.getAddress() );
        info.setEmail( orgUnit.getEmail() );
        info.setPhoneNumber( orgUnit.getPhoneNumber() );

        if ( orgUnit.getGeometry() != null )
        {
            if ( orgUnit.getGeometry().getGeometryType().equals( "Point" ) )
            {
                info.setLongitude( orgUnit.getGeometry().getCoordinate().x );
                info.setLatitude( orgUnit.getGeometry().getCoordinate().y );
            }
            else
            {
                Point point = orgUnit.getGeometry().getInteriorPoint();
                info.setLongitude( point.getX() );
                info.setLatitude( point.getY() );
            }
        }

        return info;
    }

    private List<ProfileItem> getAttributes( OrgUnitProfile profile, OrganisationUnit orgUnit )
    {
        List<ProfileItem> items = new ArrayList<>();

        if ( CollectionUtils.isEmpty( profile.getAttributes() ) )
        {
            return items;
        }

        List<Attribute> attributes = idObjectManager.getByUid( Attribute.class, profile.getAttributes() );

        for ( Attribute attribute : attributes )
        {
            AttributeValue attributeValue = orgUnit.getAttributeValue( attribute );

            items.add( new ProfileItem( attribute.getUid(), attribute.getDisplayName(),
                attributeValue.getValue() ) );
        }

        return items;
    }

    private List<ProfileItem> getGroupSets( OrgUnitProfile profile, OrganisationUnit orgUnit )
    {
        List<ProfileItem> items = new ArrayList<>();

        if ( CollectionUtils.isEmpty( profile.getGroupSets() ) )
        {
            return items;
        }

        List<OrganisationUnitGroupSet> groupSets = idObjectManager
            .getByUid( OrganisationUnitGroupSet.class, profile.getGroupSets() );

        Set<OrganisationUnitGroup> groups = orgUnit.getGroups();

        for ( OrganisationUnitGroupSet groupSet : groupSets )
        {
            OrganisationUnitGroup group = groupService.getOrgUnitGroupInGroupSet( groups, groupSet );

            items.add( new ProfileItem( groupSet.getUid(), groupSet.getDisplayName(), group.getDisplayName() ) );
        }

        return items;
    }

    private List<ProfileItem> getDataItems( OrgUnitProfile profile, OrganisationUnit orgUnit, Period period )
    {
        List<ProfileItem> items = new ArrayList<>();

        if ( CollectionUtils.isEmpty( profile.getDataItems() ) )
        {
            return items;
        }

        List<DimensionalItemObject> dataItems = idObjectManager.getByUid( DATA_ITEM_CLASSES, profile.getDataItems() );

        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataDimensionItems( dataItems )
            .withFilterOrganisationUnit( orgUnit )
            .withFilterPeriod( period )
            .build();

        Map<String, Object> values = analyticsService.getAggregatedDataValueMapping( params );

        for ( DimensionalItemObject dataItem : dataItems )
        {
            Object value = values.get( dataItem.getUid() );

            items.add( new ProfileItem( dataItem.getUid(), dataItem.getDisplayName(), value ) );
        }

        return items;
    }

    private OrganisationUnit getOrgUnit( String orgUnit )
    {
        OrganisationUnit unit = idObjectManager.get( OrganisationUnit.class, orgUnit );

        if ( unit == null )
        {
            throw new IllegalQueryException( ErrorCode.E1102 );
        }

        return unit;
    }

    /**
     * Returns the a period based on the given ISO period string. If the ISO
     * period is not defined or invalid, the current year is used as fall back.
     *
     * @param isoPeriod the ISO period string, can be null.
     * @return a {@link Period}.
     */
    private Period getPeriod( String isoPeriod )
    {
        Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

        if ( period != null )
        {
            return period;
        }
        else
        {
            return RelativePeriods
                .getRelativePeriodsFromEnum(
                    RelativePeriodEnum.THIS_YEAR, new Date() )
                .get( 0 );
        }
    }

}
