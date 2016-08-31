package org.hisp.dhis.jdbc.statementbuilder;

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

import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class MySQLStatementBuilder
    extends AbstractStatementBuilder
{    
    @Override
    public String getDoubleColumnType()
    {
        return "decimal(26,1)";
    }

    @Override
    public String getLongVarBinaryType()
    {
        return "BLOB";
    }

    @Override
    public String getColumnQuote()
    {
        return "`";
    }

    @Override
    public String getVacuum( String table )
    {
        return "optimize table " + table + ";";
    }

    @Override
    public String getTableOptions( boolean autoVacuum )
    {
        return ""; //TODO implement
    }

    @Override
    public String getRegexpMatch()
    {
        return "regexp";
    }

    @Override
    public String getRegexpWordStart()
    {
        return "[[:<:]]";
    }

    @Override
    public String getRegexpWordEnd()
    {
        return "[[:>:]]";
    }

    @Override
    public String getRandom( int n )
    {
        return "cast(floor(" + n + "*rand()) as integer)";
    }

    @Override
    public String getCharAt( String str, String n )
    {
        return "substring(" + str + "," + n + ",1)";
    }

    @Override
    public String getCastToDate( String column )
    {
        return "date(" + column + ")";
    }

    @Override
    public String getDeleteZeroDataValues()
    {
        return
            "DELETE FROM datavalue " +
            "USING datavalue, dataelement " +
            "WHERE datavalue.dataelementid = dataelement.dataelementid " +
            "AND dataelement.aggregationtype = 'sum' " +
            "AND dataelement.zeroissignificant = false " +
            "AND datavalue.value = '0'";
    }

    @Override
    public String getAddDate( String dateField, int days )
    {
        return "ADDDATE(" + dateField + "," + days + ")";
    }

    @Override
    public String queryDataElementStructureForOrgUnit()
    {
        StringBuilder sqlsb = new StringBuilder();
        
        sqlsb.append( "(SELECT DISTINCT de.dataelementid, concat(de.name, \" \", cc.name) AS DataElement " );
        sqlsb.append( "FROM dataelement AS de " );
        sqlsb.append( "INNER JOIN categorycombos_optioncombos cat_opts on de.categorycomboid = cat_opts.categorycomboid ");
        sqlsb.append( "INNER JOIN categoryoptioncombo cc on cat_opts.categoryoptioncomboid = cc.categoryoptioncomboid ");
        sqlsb.append( "ORDER BY DataElement) " );
        
        return sqlsb.toString();
    }

    @Override
    public String queryRawDataElementsForOrgUnitBetweenPeriods(Integer orgUnitId, List<Integer> betweenPeriodIds)
    {
        StringBuilder sqlsb = new StringBuilder();

        int i = 0;
        
        for ( Integer periodId : betweenPeriodIds )
        {
            i++;

            sqlsb.append( "SELECT de.dataelementid, concat(de.name, \" \" , cc.name) AS DataElement, dv.value AS counts_of_aggregated_values, p.periodid AS PeriodId, p.startDate AS ColumnHeader " );
            sqlsb.append( "FROM dataelement AS de " );
            sqlsb.append( "INNER JOIN datavalue AS dv ON (de.dataelementid = dv.dataelementid) " );
            sqlsb.append( "INNER JOIN period p ON (dv.periodid = p.periodid) " );
            sqlsb.append( "INNER JOIN categorycombos_optioncombos cat_opts on de.categorycomboid = cat_opts.categorycomboid ");
            sqlsb.append( "INNER JOIN categoryoptioncombo cc on cat_opts.categoryoptioncomboid = cc.categoryoptioncomboid ");
            sqlsb.append( "WHERE dv.sourceid = " + orgUnitId + " " );
            sqlsb.append( "AND dv.periodid = " + periodId + " " );

            sqlsb.append( i == betweenPeriodIds.size() ? "ORDER BY ColumnHeader,dataelement" : " UNION " );
        }
        
        return sqlsb.toString();
    }    
}
