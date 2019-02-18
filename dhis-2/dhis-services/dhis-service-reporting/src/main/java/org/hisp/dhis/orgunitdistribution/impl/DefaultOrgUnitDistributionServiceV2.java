package org.hisp.dhis.orgunitdistribution.impl;

import java.util.HashMap;
import java.util.Map;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.Set;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionManager;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionParams;
import org.hisp.dhis.orgunitdistribution.OrgUnitDistributionServiceV2;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DefaultOrgUnitDistributionServiceV2
    implements OrgUnitDistributionServiceV2
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private OrgUnitDistributionManager distributionManager;

    @Override
    public OrgUnitDistributionParams getParams( Set<String> orgUnits, Set<String> orgUnitGroupSets )
    {
        return new OrgUnitDistributionParams()
            .setOrgUnits( idObjectManager.getObjects( OrganisationUnit.class, IdentifiableProperty.UID, orgUnits ) )
            .setOrgUnitGroupSets( idObjectManager.getObjects( OrganisationUnitGroupSet.class, IdentifiableProperty.UID, orgUnitGroupSets ) );
    }

    @Override
    public Grid getOrgUnitDistribution( OrgUnitDistributionParams params )
    {
        validate( params );

        Grid grid = new ListGrid();

        addHeaders( params, grid );
        addMetadata( params, grid );

        distributionManager.getOrgUnitDistribution( params, grid );

        return grid;
    }

    @Override
    public void validate( OrgUnitDistributionParams params )
    {
        if ( params == null )
        {
            throw new IllegalQueryException( "Query cannot be null" );
        }

        if ( params.getOrgUnits().isEmpty() )
        {
            throw new IllegalQueryException( "At least one org unit must be specified" );
        }

        if ( params.getOrgUnitGroupSets().isEmpty() )
        {
            throw new IllegalQueryException( "At least one org unit group set must be specified" );
        }
    }

    private void addHeaders( OrgUnitDistributionParams params, Grid grid )
    {
        grid.addHeader( new GridHeader( "orgunit", "Organisation unit", ValueType.TEXT, null, false, true ) );
        params.getOrgUnitGroupSets().forEach( ougs ->
            grid.addHeader( new GridHeader( ougs.getUid(), ougs.getDisplayName(), ValueType.TEXT, null, false, true ) ) );
        grid.addHeader( new GridHeader( "count", "Count", ValueType.INTEGER, null, false, false ) );
    }

    private void addMetadata( OrgUnitDistributionParams params, Grid grid )
    {
        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> items = new HashMap<>();

        params.getOrgUnits().stream()
            .forEach( ou -> items.put( ou.getUid(), getItem( ou ) ) );
        params.getOrgUnitGroupSets().stream()
            .forEach( ougs -> items.put( ougs.getUid(), getItem( ougs ) ) );

        metadata.put( "items", items );
        grid.setMetaData( metadata );
    }

    private IdentifiableObject getItem( IdentifiableObject item )
    {
        BaseIdentifiableObject object = new BaseIdentifiableObject();
        object.setName( item.getDisplayName() );
        return object;
    }
}
