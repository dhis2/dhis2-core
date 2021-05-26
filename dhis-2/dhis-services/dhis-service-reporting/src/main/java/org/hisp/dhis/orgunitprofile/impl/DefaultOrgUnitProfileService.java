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

import java.util.List;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
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

    public OrgUnitProfileData getOrgUnitProfileData( String uid )
    {
        OrgUnitProfile profile = getOrgUnitProfile();

        if ( profile == null )
        {
            // throw new IllegalQueryException with new error code
        }

        OrganisationUnit orgUnit = idObjectManager.get( OrganisationUnit.class, uid );

        if ( orgUnit == null )
        {
            throw new IllegalQueryException( ErrorCode.E1102 );
        }

        OrgUnitProfileData data = new OrgUnitProfileData();

        // Populate info

        OrgUnitInfo info = getOrgUnitInfo( orgUnit );

        data.setInfo( info );

        // Populate attributes

        List<Attribute> attributes = idObjectManager.get( Attribute.class, profile.getAttributes() );

        for ( Attribute attribute : attributes )
        {
            AttributeValue attributeValue = orgUnit.getAttributeValue( attribute );

            ProfileItem item = new ProfileItem( attribute.getUid(), attribute.getDisplayName(),
                attributeValue.getValue() );

            data.getAttributes().add( item );
        }

        // Populate data items

        // idObjectManager.get( DATA_ITEM_CLASSES, profile.getDataItems() );

        return data;
    }

    public OrgUnitInfo getOrgUnitInfo( OrganisationUnit unit )
    {
        OrgUnitInfo info = new OrgUnitInfo();
        // Populate info
        return info;
    }
}
