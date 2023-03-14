/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.resourcetable.table;

import static org.hisp.dhis.system.util.SqlUtils.quote;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class IndicatorGroupSetResourceTable
    extends ResourceTable<IndicatorGroupSet>
{
    private final String tableType;

    public IndicatorGroupSetResourceTable( List<IndicatorGroupSet> objects, String tableType )
    {
        super( objects );
        this.tableType = tableType;
    }

    @Override
    public ResourceTableType getTableType()
    {
        return ResourceTableType.INDICATOR_GROUP_SET_STRUCTURE;
    }

    @Override
    public String getCreateTempTableStatement()
    {
        String statement = "create " + tableType + " table " + getTempTableName() + " (" +
            "indicatorid bigint not null, " +
            "indicatorname varchar(230), ";

        for ( IndicatorGroupSet groupSet : objects )
        {
            statement += quote( groupSet.getShortName() ) + " varchar(230), ";
            statement += quote( groupSet.getUid() ) + " character(11), ";
        }

        statement += "primary key (indicatorid))";

        return statement;
    }

    @Override
    public Optional<String> getPopulateTempTableStatement()
    {
        String sql = "insert into " + getTempTableName() + " " +
            "select i.indicatorid as indicatorid, i.name as indicatorname, ";

        for ( IndicatorGroupSet groupSet : objects )
        {
            sql += "(" +
                "select ig.name from indicatorgroup ig " +
                "inner join indicatorgroupmembers igm on igm.indicatorgroupid = ig.indicatorgroupid " +
                "inner join indicatorgroupsetmembers igsm on " +
                "igsm.indicatorgroupid = igm.indicatorgroupid and igsm.indicatorgroupsetid = " +
                groupSet.getId() + " " +
                "where igm.indicatorid = i.indicatorid " +
                "limit 1) as " + quote( groupSet.getName() ) + ", ";

            sql += "(" +
                "select ig.uid from indicatorgroup ig " +
                "inner join indicatorgroupmembers igm on " +
                "igm.indicatorgroupid = ig.indicatorgroupid " +
                "inner join indicatorgroupsetmembers igsm on " +
                "igsm.indicatorgroupid = igm.indicatorgroupid and igsm.indicatorgroupsetid = " +
                groupSet.getId() + " " +
                "where igm.indicatorid = i.indicatorid " +
                "limit 1) as " + quote( groupSet.getUid() ) + ", ";
        }

        sql = TextUtils.removeLastComma( sql ) + " ";
        sql += "from indicator i";

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
