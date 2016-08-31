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
public class HsqlStatementBuilder
    extends AbstractStatementBuilder
{
    @Override
    public String getDoubleColumnType()
    {
        return "double";
    }

    @Override
    public String getColumnQuote()
    {
        return "\"";
    }

    @Override
    public String getVacuum( String table )
    {
        return null;
    }

    @Override
    public String getTableOptions( boolean autoVacuum )
    {
        return "";
    }

    @Override
    public String getRegexpMatch()
    {
        return "regexp";
    }

    @Override
    public String getRegexpWordStart() //TODO test
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
    public String getDeleteZeroDataValues()
    {
        return
            "DELETE FROM datavalue " +
            "WHERE datavalue.dataelementid IN (" +
                "SELECT dataelementid from dataelement " +
                "WHERE dataelement.aggregationtype = 'sum' " +
                "AND dataelement.zeroissignificant = false) " +
            "AND datavalue.value = '0'";
    }

    @Override
    public String getMoveDataValueToDestination( int sourceId, int destinationId )
    {
        return "UPDATE datavalue AS d1 SET sourceid=" + destinationId + " " + "WHERE sourceid=" + sourceId + " "
            + "AND NOT EXISTS ( " + "SELECT * from datavalue AS d2 " + "WHERE d2.sourceid=" + destinationId + " "
            + "AND d1.dataelementid=d2.dataelementid " + "AND d1.periodid=d2.periodid "
            + "AND d1.categoryoptioncomboid=d2.categoryoptioncomboid );";
    }

    @Override
    public String getSummarizeDestinationAndSourceWhereMatching( int sourceId, int destId )
    {
        return "UPDATE datavalue AS d1 SET value=( " + "SELECT SUM( CAST( value AS "
            + getDoubleColumnType() + " ) ) " + "FROM datavalue as d2 "
            + "WHERE d1.dataelementid=d2.dataelementid " + "AND d1.periodid=d2.periodid "
            + "AND d1.categoryoptioncomboid=d2.categoryoptioncomboid " + "AND d2.sourceid IN ( " + destId + ", "
            + sourceId + " ) ) " + "FROM dataelement AS de " + "WHERE d1.sourceid=" + destId + " "
            + "AND d1.dataelementid=de.dataelementid " + "AND de.valuetype='int';";
    }

    @Override
    public String getUpdateDestination( int destDataElementId, int destCategoryOptionComboId,
        int sourceDataElementId, int sourceCategoryOptionComboId )
    {
        return "UPDATE datavalue AS d1 SET dataelementid=" + destDataElementId + ", categoryoptioncomboid="
            + destCategoryOptionComboId + " " + "WHERE dataelementid=" + sourceDataElementId
            + " and categoryoptioncomboid=" + sourceCategoryOptionComboId + " " + "AND NOT EXISTS ( "
            + "SELECT * FROM datavalue AS d2 " + "WHERE d2.dataelementid=" + destDataElementId + " "
            + "AND d2.categoryoptioncomboid=" + destCategoryOptionComboId + " " + "AND d1.periodid=d2.periodid "
            + "AND d1.sourceid=d2.sourceid );";
    }

    @Override
    public String getMoveFromSourceToDestination( int destDataElementId, int destCategoryOptionComboId,
        int sourceDataElementId, int sourceCategoryOptionComboId )
    {
        return "UPDATE datavalue SET value=d2.value,storedby=d2.storedby,lastupdated=d2.lastupdated,comment=d2.comment,followup=d2.followup "
            + "FROM datavalue AS d2 "
            + "WHERE datavalue.periodid=d2.periodid "
            + "AND datavalue.sourceid=d2.sourceid "
            + "AND datavalue.lastupdated<d2.lastupdated "
            + "AND datavalue.dataelementid="
            + destDataElementId
            + " AND datavalue.categoryoptioncomboid="
            + destCategoryOptionComboId + " "
            + "AND d2.dataelementid="
            + sourceDataElementId + " AND d2.categoryoptioncomboid=" + sourceCategoryOptionComboId + ";";
    }

    @Override
    public String getStandardDeviation( int dataElementId, int categoryOptionComboId, int organisationUnitId ){
        
        return "SELECT STDDEV( CAST( value AS " + getDoubleColumnType() + " ) ) FROM datavalue " +
            "WHERE dataelementid='" + dataElementId + "' " +
            "AND categoryoptioncomboid='" + categoryOptionComboId + "' " +
            "AND sourceid='" + organisationUnitId + "'";
    }

    @Override
    public String getAverage( int dataElementId, int categoryOptionComboId, int organisationUnitId )
    {    
         return "SELECT AVG( CAST( value AS " + getDoubleColumnType() + " ) ) FROM datavalue " +
             "WHERE dataelementid='" + dataElementId + "' " +
             "AND categoryoptioncomboid='" + categoryOptionComboId + "' " +
             "AND sourceid='" + organisationUnitId + "'";
    }

    @Override
    public String getAddDate( String dateField, int days )
    {
        return "DATEADD('DAY'," + days + "," + dateField + ")";
    }

    @Override
    public String queryDataElementStructureForOrgUnit()
    {
        StringBuilder sqlsb = new StringBuilder();
        
        sqlsb.append( "(SELECT DISTINCT de.dataelementid, (de.name || ' ' || cc.name) AS DataElement " );
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

            sqlsb.append( "SELECT de.dataelementid, (de.name || ' ' || cc.name) AS DataElement, dv.value AS counts_of_aggregated_values, p.periodid AS PeriodId, p.startDate AS ColumnHeader " );
            sqlsb.append( "FROM dataelement AS de " );
            sqlsb.append( "INNER JOIN datavalue AS dv ON (de.dataelementid = dv.dataelementid) " );
            sqlsb.append( "INNER JOIN period p ON (dv.periodid = p.periodid) " );
            sqlsb.append( "INNER JOIN categorycombos_optioncombos cat_opts on de.categorycomboid = cat_opts.categorycomboid ");
            sqlsb.append( "INNER JOIN categoryoptioncombo cc on cat_opts.categoryoptioncomboid = cc.categoryoptioncomboid ");
            sqlsb.append( "WHERE dv.sourceid = '" + orgUnitId + "' " );
            sqlsb.append( "AND dv.periodid = '" + periodId + "' " );

            sqlsb.append( i == betweenPeriodIds.size() ? "ORDER BY ColumnHeader,dataelement" : " UNION " );
        }
        
        return sqlsb.toString();
    }
}
