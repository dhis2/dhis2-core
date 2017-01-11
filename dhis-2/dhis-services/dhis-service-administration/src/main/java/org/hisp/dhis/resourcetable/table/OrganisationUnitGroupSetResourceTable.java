package org.hisp.dhis.resourcetable.table;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.List;
import java.util.Optional;

/**
 * @author Lars Helge Overland
 */
public class OrganisationUnitGroupSetResourceTable
    extends ResourceTable<OrganisationUnitGroupSet>
{
    public OrganisationUnitGroupSetResourceTable( List<OrganisationUnitGroupSet> objects, String columnQuote )
    {
        super( objects, columnQuote );
    }

    @Override
    public String getTableName()
    {
        return "_organisationunitgroupsetstructure";
    }
    
    @Override
    public String getCreateTempTableStatement()
    {
        String statement = "create table " + getTempTableName() + " (" +
            "organisationunitid integer not null, " +
            "organisationunitname varchar(230), ";
                
        for ( OrganisationUnitGroupSet groupSet : objects )
        {
            statement += columnQuote + groupSet.getName() + columnQuote + " varchar(230), ";
            statement += columnQuote + groupSet.getUid() + columnQuote + " character(11), ";
        }
        
        statement += "primary key (organisationunitid))";
        
        return statement;
    }

    @Override
    public Optional<String> getPopulateTempTableStatement()
    {
        String sql = 
            "insert into " + getTempTableName() + " " +
            "select ou.organisationunitid as organisationunitid, ou.name as organisationunitname, ";            
        
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
                sql += "(" +
                    "select oug.name " +
                    "from orgunitgroup oug " +
                    "inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid " +
                    "inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid and ougsm.orgunitgroupsetid = " + groupSet.getId() + " " +
                    "inner join organisationunit ou2 ON ou2.organisationunitid = ougm.organisationunitid AND ou.path LIKE concat(ou2.path, '%') " +
                    "where ougm.orgunitgroupid is not null " +
                    "order by hierarchylevel desc " +
                    "limit 1) as " + columnQuote + groupSet.getName() + columnQuote + ", ";

                sql += "(" +
                    "select oug.uid " +
                    "from orgunitgroup oug " +
                    "inner join orgunitgroupmembers ougm on ougm.orgunitgroupid = oug.orgunitgroupid " +
                    "inner join orgunitgroupsetmembers ougsm on ougsm.orgunitgroupid = ougm.orgunitgroupid and ougsm.orgunitgroupsetid = " + groupSet.getId() + " " +
                    "inner join organisationunit ou2 ON ou2.organisationunitid = ougm.organisationunitid AND ou.path LIKE concat(ou2.path, '%') " +
                    "where ougm.orgunitgroupid is not null " +
                    "order by hierarchylevel desc " +
                    "limit 1) as " + columnQuote + groupSet.getName() + columnQuote + ", ";
            }
        }
        
        sql = TextUtils.removeLastComma( sql ) + " ";
        sql += "from organisationunit ou";
        
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
        return Lists.newArrayList();
    }
}
