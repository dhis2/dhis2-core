package org.hisp.dhis.resourcetable.table;

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

import com.google.common.collect.Lists;

import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

import java.util.List;
import java.util.Optional;

import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.system.util.SqlUtils.quote;

/**
 * @author Lars Helge Overland
 */
public class OrganisationUnitGroupSetResourceTable
    extends ResourceTable<OrganisationUnitGroupSet>
{
    private boolean supportsPartialIndexes;

    private int organisationUnitLevels;

    public OrganisationUnitGroupSetResourceTable( List<OrganisationUnitGroupSet> objects,
        String columnQuote, boolean supportsPartialIndexes, int organisationUnitLevels )
    {
        super( objects, columnQuote );
        this.supportsPartialIndexes = supportsPartialIndexes;
        this.organisationUnitLevels = organisationUnitLevels;
    }

    @Override
    public ResourceTableType getTableType()
    {
        return ResourceTableType.ORG_UNIT_GROUP_SET_STRUCTURE;
    }
    
    @Override
    public String getCreateTempTableStatement()
    {
        String statement = "create table " + getTempTableName() + " (" +
            "organisationunitid integer not null, " +
            "organisationunitname varchar(230), " +
            "startdate date, ";
        
        for ( OrganisationUnitGroupSet groupSet : objects )
        {
            statement += columnQuote + groupSet.getName() + columnQuote + " varchar(230), ";
            statement += columnQuote + groupSet.getUid() + columnQuote + " character(11), ";
        }
        
        return removeLastComma( statement ) + ")";
    }

    @Override
    public Optional<String> getPopulateTempTableStatement()
    {
        String sql = 
            "insert into " + getTempTableName() + " " +
            "select ou.organisationunitid as organisationunitid, ou.name as organisationunitname, null as startdate, ";
        
        for ( OrganisationUnitGroupSet groupSet : objects )
        {
            if ( !groupSet.isIncludeSubhierarchyInAnalytics() )
            {
                sql += "(" +
                    "select oug.name from orgunitgroup oug " +
                    "inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid " +
                    "inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid and ougsm.orgunitgroupsetid = " +
                    groupSet.getId() + " " +
                    "where ougm.organisationunitid = ou.organisationunitid " +
                    "limit 1) as " + columnQuote + groupSet.getName() + columnQuote + ", ";

                sql += "(" +
                    "select oug.uid from orgunitgroup oug " +
                    "inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid " +
                    "inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid and ougsm.orgunitgroupsetid = " +
                    groupSet.getId() + " " +
                    "where ougm.organisationunitid = ou.organisationunitid " +
                    "limit 1) as " + columnQuote + groupSet.getUid() + columnQuote + ", ";
            }
            else
            {
                sql += "coalesce(";

                for ( int i = organisationUnitLevels; i > 0; i-- )
                {
                    sql += "(select oug.name from orgunitgroup oug " +
                        "inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid and ougm.organisationunitid = ous.idlevel" + i + " " +
                        "inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid and ougsm.orgunitgroupsetid = " + groupSet.getId() + " " +
                        "limit 1),";
                }

                if ( organisationUnitLevels == 0 )
                {
                    sql += "null";
                }

                sql = removeLastComma( sql ) +
                    ") as " + columnQuote + groupSet.getName() + columnQuote + ", ";

                sql += "coalesce(";

                for ( int i = organisationUnitLevels; i > 0; i-- )
                {
                    sql += "(select oug.uid from orgunitgroup oug " +
                        "inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid and ougm.organisationunitid = ous.idlevel" + i + " " +
                        "inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid and ougsm.orgunitgroupsetid = " + groupSet.getId() + " " +
                        "limit 1),";
                }

                if ( organisationUnitLevels == 0 )
                {
                    sql += "null";
                }

                sql = removeLastComma( sql ) +
                    ") as " + columnQuote + groupSet.getUid() + columnQuote + ", ";
            }
        }
        
        sql = removeLastComma( sql ) + " ";
        sql += "from organisationunit ou " +
            "inner join _orgunitstructure ous on ous.organisationunitid = ou.organisationunitid";
        
        return Optional.of( sql );
    }

    @Override
    public Optional<List<Object[]>> getPopulateTempTableContent()
    {
        return Optional.empty();
    }

    @Override
    public List<String> getCreateIndexStatements()
    {
        String nameA = "in_orgunitgroupsetstructure_not_null_" + getRandomSuffix();
        String nameB = "in_orgunitgroupsetstructure_null_" + getRandomSuffix();

        // Two partial indexes as start date can be null
        
        String indexA = "create index " + nameA + " on " + getTempTableName() + "(organisationunitid, startdate) " + 
            TextUtils.emptyIfFalse( "where startdate is not null", supportsPartialIndexes );
        String indexB = "create index " + nameB + " on " + getTempTableName() + "(organisationunitid, startdate) " + 
            TextUtils.emptyIfFalse( "where startdate is null", supportsPartialIndexes );

        return Lists.newArrayList( indexA, indexB );
    }
}
