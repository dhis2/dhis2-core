package org.hisp.dhis.dxf2.datavalueset;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.system.util.DateUtils.getLongGmtDateString;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

import java.io.OutputStream;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.amplecode.staxwax.factory.XMLFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.IdSchemes;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.csvreader.CsvWriter;

/**
 * @author Lars Helge Overland
 */
public class SpringDataValueSetStore
    implements DataValueSetStore
{
    private static final Log log = LogFactory.getLog( SpringDataValueSetStore.class );
    
    private static final char CSV_DELIM = ',';

    @Autowired
    private JdbcTemplate jdbcTemplate;

    //--------------------------------------------------------------------------
    // DataValueSetStore implementation
    //--------------------------------------------------------------------------

    @Override
    public void writeDataValueSetXml( DataExportParams params, Date completeDate, OutputStream out )
    {
        DataValueSet dataValueSet = new StreamingDataValueSet( XMLFactory.getXMLWriter( out ) );

        writeDataValueSet( getDataValueSql( params ), params, completeDate, dataValueSet );

        IOUtils.closeQuietly( out );
    }

    @Override
    public void writeDataValueSetJson( DataExportParams params, Date completeDate, OutputStream out )
    {
        DataValueSet dataValueSet = new StreamingJsonDataValueSet( out );

        writeDataValueSet( getDataValueSql( params ), params, completeDate, dataValueSet );

        IOUtils.closeQuietly( out );
    }

    @Override
    public void writeDataValueSetCsv( DataExportParams params, Date completeDate, Writer writer )
    {
        DataValueSet dataValueSet = new StreamingCsvDataValueSet( new CsvWriter( writer, CSV_DELIM ) );

        writeDataValueSet( getDataValueSql( params ), params, completeDate, dataValueSet );

        IOUtils.closeQuietly( writer );
    }

    @Override
    public void writeDataValueSetJson( Date lastUpdated, OutputStream outputStream, IdSchemes idSchemes )
    {
        String deScheme = idSchemes.getDataElementIdScheme().toString().toLowerCase();
        String ouScheme = idSchemes.getOrgUnitIdScheme().toString().toLowerCase();
        String ocScheme = idSchemes.getCategoryOptionComboIdScheme().toString().toLowerCase();

        DataValueSet dataValueSet = new StreamingJsonDataValueSet( outputStream );

        final String sql =
            "select de." + deScheme + " as deid, pe.startdate as pestart, pt.name as ptname, ou." + ouScheme + " as ouid, " +
            "coc." + ocScheme + " as cocid, aoc." + ocScheme + " as aocid, " +
            "dv.value, dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup " +
            "from datavalue dv " +
            "join dataelement de on (dv.dataelementid=de.dataelementid) " +
            "join period pe on (dv.periodid=pe.periodid) " +
            "join periodtype pt on (pe.periodtypeid=pt.periodtypeid) " +
            "join organisationunit ou on (dv.sourceid=ou.organisationunitid) " +
            "join categoryoptioncombo coc on (dv.categoryoptioncomboid=coc.categoryoptioncomboid) " +
            "join categoryoptioncombo aoc on (dv.attributeoptioncomboid=aoc.categoryoptioncomboid) " +
            "where dv.lastupdated >= '" + DateUtils.getLongDateString( lastUpdated ) + "'";

        writeDataValueSet( sql, new DataExportParams(), null, dataValueSet );
    }

    private void writeDataValueSet( String sql, DataExportParams params, Date completeDate, final DataValueSet dataValueSet )
    {
        if ( params.isSingleDataValueSet() )
        {
            dataValueSet.setDataSet( params.getFirstDataSet().getUid() ); //TODO id scheme
            dataValueSet.setCompleteDate( getLongGmtDateString( completeDate ) );
            dataValueSet.setPeriod( params.getFirstPeriod().getIsoDate() );
            dataValueSet.setOrgUnit( params.getFirstOrganisationUnit().getUid() );
        }

        final Calendar calendar = PeriodType.getCalendar();

        jdbcTemplate.query( sql, new RowCallbackHandler()
        {
            @Override
            public void processRow( ResultSet rs ) throws SQLException
            {
                DataValue dataValue = dataValueSet.getDataValueInstance();
                PeriodType pt = PeriodType.getPeriodTypeByName( rs.getString( "ptname" ) );

                dataValue.setDataElement( rs.getString( "deid" ) );
                dataValue.setPeriod( pt.createPeriod( rs.getDate( "pestart" ), calendar ).getIsoDate() );
                dataValue.setOrgUnit( rs.getString( "ouid" ) );
                dataValue.setCategoryOptionCombo( rs.getString( "cocid" ) );
                dataValue.setAttributeOptionCombo( rs.getString( "aocid" ) );
                dataValue.setValue( rs.getString( "value" ) );
                dataValue.setStoredBy( rs.getString( "storedby" ) );
                dataValue.setCreated( getLongGmtDateString( rs.getTimestamp( "created" ) ) );
                dataValue.setLastUpdated( getLongGmtDateString( rs.getTimestamp( "lastupdated" ) ) );
                dataValue.setComment( rs.getString( "comment" ) );
                dataValue.setFollowup( rs.getBoolean( "followup" ) );
                dataValue.close();
            }
        } );

        dataValueSet.close();
    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private String getDataValueSql( DataExportParams params )
    {
        IdSchemes idSchemes = params.getIdSchemes() != null ? params.getIdSchemes() : new IdSchemes();

        String deScheme = idSchemes.getDataElementIdScheme().toString().toLowerCase();
        String ouScheme = idSchemes.getOrgUnitIdScheme().toString().toLowerCase();
        String ocScheme = idSchemes.getCategoryOptionComboIdScheme().toString().toLowerCase();

        String sql =
            "select de." + deScheme + " as deid, pe.startdate as pestart, pt.name as ptname, ou." + ouScheme + " as ouid, " +
            "coc." + ocScheme + " as cocid, aoc." + ocScheme + " as aocid, " +
            "dv.value, dv.storedby, dv.created, dv.lastupdated, dv.comment, dv.followup " +
            "from datavalue dv " +
            "join dataelement de on (dv.dataelementid=de.dataelementid) " +
            "join period pe on (dv.periodid=pe.periodid) " +
            "join periodtype pt on (pe.periodtypeid=pt.periodtypeid) " +
            "join organisationunit ou on (dv.sourceid=ou.organisationunitid) " +
            "join categoryoptioncombo coc on (dv.categoryoptioncomboid=coc.categoryoptioncomboid) " +
            "join categoryoptioncombo aoc on (dv.attributeoptioncomboid=aoc.categoryoptioncomboid) " +
            "where de.dataelementid in (" + getCommaDelimitedString( getIdentifiers( getDataElements( params.getDataSets() ) ) ) + ") ";
        
        if ( params.isIncludeChildren() )
        {
            sql += "and (";
            
            for ( OrganisationUnit parent : params.getOrganisationUnits() )
            {
                sql += "ou.path like '" + parent.getPath() + "%' or ";
            }
            
            sql = TextUtils.removeLastOr( sql ) + ")";            
        }
        else
        {
            sql += "and dv.sourceid in (" + getCommaDelimitedString( getIdentifiers( params.getOrganisationUnits() ) ) + ") ";
        }
        
        if ( params.hasStartEndDate() )
        {
            sql += "and (pe.startdate >= '" + getMediumDateString( params.getStartDate() ) + "' and pe.enddate <= '" + getMediumDateString( params.getEndDate() ) + "') ";
        }
        else
        {
            sql += "and dv.periodid in (" + getCommaDelimitedString( getIdentifiers( params.getPeriods() ) ) + ") ";
        }
        
        if ( params.hasLastUpdated() )
        {
            sql += "and dv.lastupdated >= '" + getLongGmtDateString( params.getLastUpdated() ) + "' ";
        }
        
        if ( params.hasLimit() )
        {
            sql += "limit " + params.getLimit();
        }
        
        log.debug( "Get data value set SQL: " + sql );
        
        return sql;
    }

    private Set<DataElement> getDataElements( Set<DataSet> dataSets )
    {
        Set<DataElement> elements = new HashSet<>();

        for ( DataSet dataSet : dataSets )
        {
            elements.addAll( dataSet.getDataElements() );
        }

        return elements;
    }
}
