package org.hisp.dhis.dxf2.dataset;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.OutputStream;
import java.util.Date;

/**
 * @author Halvdan Hoem Grelland
 */
public class SpringCompleteDataSetRegistrationStore
    implements CompleteDataSetRegistrationStore
{
    private static final Log log = LogFactory.getLog( SpringCompleteDataSetRegistrationStore.class );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    //--------------------------------------------------------------------------
    // CompleteDataSetRegistrationStore implementation
    //--------------------------------------------------------------------------

    @Override
    public void writeCompleteDataSetRegistrationsXml( ExportParams params, OutputStream outputStream )
    {

    }

    @Override
    public void writeCompleteDataSetRegistrationsJson( ExportParams params, OutputStream outputStream )
    {

    }

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private String createQuery( ExportParams params )
    {
        IdSchemes idSchemes = params.getOutputIdSchemes() != null ? params.getOutputIdSchemes() : new IdSchemes();



        return "";
    }

    private void write( String query, ExportParams params, final CompleteDataSetRegistrations items )
    {
        final Calendar calendar = PeriodType.getCalendar();

        jdbcTemplate.query( query, rs ->
        {
            CompleteDataSetRegistration cdsr = new CompleteDataSetRegistration();
            cdsr.open();

            cdsr.setPeriod( toIsoDate( rs.getString( Param.PERIOD_TYPE.name ), rs.getDate( Param.PERIOD_START.name ), calendar ) );
            cdsr.setDataSet( rs.getString( Param.DATA_SET.name ) );
            cdsr.setOrganisationUnit( rs.getString( Param.ORG_UNIT.name ) );
            cdsr.setAttributeOptionCombo( rs.getString( Param.ATTR_OPT_COMBO.name ) );
            cdsr.setDate( rs.getString( Param.DATE.name ) );
            cdsr.setStoredBy( rs.getString( Param.STORED_BY.name ) );

            cdsr.close();
        } );
    }

    private static String toIsoDate( String periodName, Date start, final Calendar calendar )
    {
        return PeriodType.getPeriodTypeByName( periodName ).createPeriod( start, calendar ).getIsoDate();
    }

    private enum Param {
        PERIOD_TYPE( "ptname" ),
        DATA_SET( "ds" ),
        ORG_UNIT( "orgunit" ),
        ATTR_OPT_COMBO( "aoc" ),
        DATE( "date" ),
        STORED_BY( "storedBy"),
        PERIOD_START( "pe_start" ),
        PERIOD_END( "pe_end" );

        final String name;

        Param( String name )
        {
            this.name = name;
        }
    }
}
