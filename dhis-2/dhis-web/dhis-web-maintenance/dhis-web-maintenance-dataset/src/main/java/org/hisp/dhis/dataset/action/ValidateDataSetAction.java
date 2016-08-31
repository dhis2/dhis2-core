package org.hisp.dhis.dataset.action;

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

import com.opensymphony.xwork2.Action;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.period.PeriodType;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Kristian
 * @version $Id: ValidateDataSetAction.java 6255 2008-11-10 16:01:24Z larshelg $
 */
public class ValidateDataSetAction
    implements Action
{
    private Integer dataSetId;

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    private String code;

    public void setCode( String code )
    {
        this.code = code;
    }

    private String periodType;

    public void setPeriodType( String periodType )
    {
        this.periodType = periodType;
    }

    private Collection<String> dataElementId = new HashSet<>();

    public void setDataElementsSelectedList( Collection<String> dataElementsSelectedList )
    {
        this.dataElementId = dataElementsSelectedList;
    }

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    // -------------------------------------------------------------------------
    // I18n
    // -------------------------------------------------------------------------

    private I18n i18n;

    public void setI18n( I18n i18n )
    {
        this.i18n = i18n;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private String message;

    public String getMessage()
    {
        return message;
    }

    // -------------------------------------------------------------------------
    // Execution
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        // ---------------------------------------------------------------------
        // Code
        // ---------------------------------------------------------------------

        if ( code != null && !code.trim().isEmpty() )
        {
            DataSet match = dataSetService.getDataSetByCode( code );

            if ( match != null && (dataSetId == null || match.getId() != dataSetId) )
            {
                message = i18n.getString( "duplicate_codes" );

                return ERROR;
            }
        }

        // ---------------------------------------------------------------------
        // Data element members
        // ---------------------------------------------------------------------

        if ( periodType != null && dataElementId != null )
        {
            PeriodType pType = PeriodType.getPeriodTypeByName( periodType );

            for ( String id : dataElementId )
            {
                DataElement dataElement = dataElementService.getDataElement( Integer.parseInt( id ) );

                if ( dataElement != null && pType != null && !pType.equals( dataElement.getPeriodType() ) )
                {
                    message = i18n.getString( "data_element_has_other_period_type_than_data_set" ) + ": " + StringEscapeUtils.escapeHtml3( dataElement.getName() );

                    return ERROR;
                }
            }
        }

        message = "OK";

        return SUCCESS;
    }
}
