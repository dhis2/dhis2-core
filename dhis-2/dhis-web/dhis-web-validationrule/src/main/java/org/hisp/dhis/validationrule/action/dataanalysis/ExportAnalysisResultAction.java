package org.hisp.dhis.validationrule.action.dataanalysis;

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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.util.SessionUtils;

import com.opensymphony.xwork2.Action;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ExportAnalysisResultAction implements Action
{
    private static final String DEFAULT_TYPE = "pdf";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String type;

    public void setType( String type )
    {
        this.type = type;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private Grid grid;

    public Grid getGrid()
    {
        return grid;
    }

    @Override
    public String execute() throws Exception
    {
        grid = generateGrid();

        type = StringUtils.defaultIfEmpty( type, DEFAULT_TYPE );

        return type;
    }

    @SuppressWarnings( "unchecked" )
    private Grid generateGrid()
    {
        List<DeflatedDataValue> results = (List<DeflatedDataValue>) SessionUtils.getSessionVar( GetAnalysisAction.KEY_ANALYSIS_DATA_VALUES );

        Grid grid = new ListGrid();
        grid.setTitle( i18n.getString( "data_analysis_report" ) );

        grid.addHeader( new GridHeader( i18n.getString( "dataelement" ), false, true ) );
        grid.addHeader( new GridHeader( i18n.getString( "source" ), false, true ) );
        grid.addHeader( new GridHeader( i18n.getString( "period" ), false, true ) );
        grid.addHeader( new GridHeader( i18n.getString( "min" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "value" ), false, false ) );
        grid.addHeader( new GridHeader( i18n.getString( "max" ), false, false ) );

        for ( DeflatedDataValue dataValue : results )
        {
            Period period = dataValue.getPeriod();

            grid.addRow();
            grid.addValue( dataValue.getDataElementName() );
            grid.addValue( dataValue.getSourceName() );
            grid.addValue( format.formatPeriod( period ) );
            grid.addValue( dataValue.getMin() );
            grid.addValue( dataValue.getValue() );
            grid.addValue( dataValue.getMax() );
        }

        return grid;
    }
}
