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

import javax.annotation.Nullable;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.orgunitprofile.OrgUnitInfo;
import org.hisp.dhis.orgunitprofile.OrgUnitProfile;
import org.hisp.dhis.orgunitprofile.OrgUnitProfileData;
import org.hisp.dhis.orgunitprofile.ProfileItem;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.program.ProgramIndicator;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

@Service
public class DefaultOrgUnitProfileService
{
    private static final List<Class<? extends IdentifiableObject>> DATA_ITEM_CLASSES = ImmutableList
        .<Class<? extends IdentifiableObject>> builder()
        .add( DataElement.class ).add( Indicator.class ).add( DataSet.class ).add( ProgramIndicator.class )
        .build();

    private KeyJsonValueService dataStore;

    private IdentifiableObjectManager idObjectManager;

    private AnalyticsService analyticsService;

    public DefaultOrgUnitProfileService( KeyJsonValueService dataStore,
        IdentifiableObjectManager idObjectManager, AnalyticsService analyticsService )
    {
        this.dataStore = dataStore;
        this.idObjectManager = idObjectManager;
        this.analyticsService = analyticsService;
    }

    public void saveOrgUnitProfile( OrgUnitProfile profile )
    {
        // Define a reserved, fixed namespace and key for the org unit profile
        // in the data store

        // Save org unit profile to data store
    }

    public OrgUnitProfile getOrgUnitProfile()
    {
        // Fetch org unit profile from data store, no manipulation

        return null;
    }

    public OrgUnitProfileData getOrgUnitProfileData( String orgUnit, @Nullable String isoPeriod )
    {
        OrgUnitProfile profile = getOrgUnitProfile();

        if ( profile == null )
        {
            throw new IllegalQueryException( ErrorCode.E1500 );
        }

        OrganisationUnit unit = idObjectManager.get( OrganisationUnit.class, orgUnit );

        if ( unit == null )
        {
            throw new IllegalQueryException( ErrorCode.E1102 );
        }

        Period period = getPeriod( isoPeriod );

        OrgUnitProfileData data = new OrgUnitProfileData();

        data.setInfo( getOrgUnitInfo( unit ) );
        data.setAttributes( getAttributes( profile, unit ) );
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

        // Set longitude and latitude based on from geometry

        return info;
    }

    private List<ProfileItem> getAttributes( OrgUnitProfile profile, OrganisationUnit orgUnit )
    {
        List<ProfileItem> items = new ArrayList<>();

        List<Attribute> attributes = idObjectManager.getByUid( Attribute.class, profile.getAttributes() );

        for ( Attribute attribute : attributes )
        {
            AttributeValue attributeValue = orgUnit.getAttributeValue( attribute );

            items.add( new ProfileItem( attribute.getUid(), attribute.getDisplayName(),
                attributeValue.getValue() ) );
        }

        return items;
    }

    private List<ProfileItem> getDataItems( OrgUnitProfile profile, OrganisationUnit orgUnit, Period period )
    {
        List<ProfileItem> items = new ArrayList<>();

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
