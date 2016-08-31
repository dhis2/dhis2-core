package org.hisp.dhis.resourcetable.table;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.resourcetable.ResourceTable;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class OrganisationUnitStructureResourceTable
    extends ResourceTable<OrganisationUnit>
{
    private OrganisationUnitService organisationUnitService; // Nasty
    
    private int organisationUnitLevels;
    
    public OrganisationUnitStructureResourceTable( List<OrganisationUnit> objects, 
        String columnQuote, OrganisationUnitService organisationUnitService, int organisationUnitLevels )
    {
        super( objects, columnQuote );
        this.organisationUnitService = organisationUnitService;
        this.organisationUnitLevels = organisationUnitLevels;
    }

    @Override
    public String getTableName()
    {
        return "_orgunitstructure";
    }
    
    @Override
    public String getCreateTempTableStatement()
    {
        StringBuilder sql = new StringBuilder();
        
        sql.append( "create table " ).append( getTempTableName() ).
            append( " (organisationunitid integer not null primary key, organisationunituid character(11), level integer" );
        
        for ( int k = 1 ; k <= organisationUnitLevels; k++ )
        {
            sql.append( ", " ).append( columnQuote ).append( "idlevel" + k ).append( columnQuote ).append (" integer, " ).
                append( columnQuote ).append( "uidlevel" + k ).append( columnQuote ).append( " character(11)" );
        }
        
        return sql.append( ");" ).toString();
    }

    @Override
    public Optional<String> getPopulateTempTableStatement()
    {
        return Optional.empty();
    }

    @Override
    public Optional<List<Object[]>> getPopulateTempTableContent()
    {
        List<Object[]> batchArgs = new ArrayList<>();

        for ( int i = 0; i < organisationUnitLevels; i++ )
        {
            int level = i + 1;

            Collection<OrganisationUnit> units = organisationUnitService.getOrganisationUnitsAtLevel( level );

            for ( OrganisationUnit unit : units )
            {
                List<Object> values = new ArrayList<>();

                values.add( unit.getId() );
                values.add( unit.getUid() );
                values.add( level );

                Map<Integer, Integer> identifiers = new HashMap<>();
                Map<Integer, String> uids = new HashMap<>();

                for ( int j = level; j > 0; j-- )
                {
                    identifiers.put( j, unit.getId() );
                    uids.put( j, unit.getUid() );

                    unit = unit.getParent();
                }

                for ( int k = 1; k <= organisationUnitLevels; k++ )
                {
                    values.add( identifiers.get( k ) != null ? identifiers.get( k ) : null );
                    values.add( uids.get( k ) );
                }

                batchArgs.add( values.toArray() );
            }
        }
        
        return Optional.of( batchArgs );
    }

    @Override
    public List<String> getCreateIndexStatements()
    {
        String name = "in_orgunitstructure_organisationunituid_" + getRandomSuffix();
        
        String sql = "create unique index " + name + " on " + getTempTableName() + "(organisationunituid)";
        
        return Lists.newArrayList( sql );
    }
}
