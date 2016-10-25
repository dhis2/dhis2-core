package org.hisp.dhis.importexport.action.datavalue;

import com.opensymphony.xwork2.Action;

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dxf2.datavalueset.DataExportParams;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.util.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

import static org.hisp.dhis.system.util.CodecUtils.filenameEncode;
import static org.hisp.dhis.system.util.DateUtils.getMediumDate;
import static org.hisp.dhis.util.ContextUtils.*;

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

/**
 * @author Lars Helge Overland
 */
public class ExportDataValueAction
    implements Action
{
    private final static String FILE_PREFIX = "Export";
    private final static String FILE_SEPARATOR = "_";
    private final static String EXTENSION_XML_ZIP = ".xml.zip";
    private final static String EXTENSION_JSON_ZIP = ".json.zip";
    private final static String EXTENSION_CSV_ZIP = ".csv.zip";
    private final static String EXTENSION_XML = ".xml";
    private final static String EXTENSION_JSON = ".json";
    private final static String EXTENSION_CSV = ".csv";
    private final static String FORMAT_CSV = "csv";
    private final static String FORMAT_JSON = "json";

    @Autowired
    private SelectionTreeManager selectionTreeManager;

    @Autowired
    private DataValueSetService dataValueSetService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Set<String> selectedDataSets;

    public void setSelectedDataSets( Set<String> selectedDataSets )
    {
        this.selectedDataSets = selectedDataSets;
    }

    private String startDate;

    public void setStartDate( String startDate )
    {
        this.startDate = startDate;
    }

    private String endDate;

    public void setEndDate( String endDate )
    {
        this.endDate = endDate;
    }

    private String exportFormat;

    public void setExportFormat( String exportFormat )
    {
        this.exportFormat = exportFormat;
    }

    private String dataElementIdScheme;

    public void setDataElementIdScheme( String dataElementIdScheme )
    {
        this.dataElementIdScheme = dataElementIdScheme;
    }

    private String orgUnitIdScheme;

    public void setOrgUnitIdScheme( String orgUnitIdScheme )
    {
        this.orgUnitIdScheme = orgUnitIdScheme;
    }

    private String categoryOptionComboIdScheme;

    public void setCategoryOptionComboIdScheme( String categoryOptionComboIdScheme )
    {
        this.categoryOptionComboIdScheme = categoryOptionComboIdScheme;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        //TODO re-implement using Web API

        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setDataElementIdScheme( StringUtils.trimToNull( dataElementIdScheme ) );
        idSchemes.setOrgUnitIdScheme( StringUtils.trimToNull( orgUnitIdScheme ) );
        idSchemes.setCategoryOptionComboIdScheme( StringUtils.trimToNull( categoryOptionComboIdScheme ) );

        Set<String> orgUnits = new HashSet<>( IdentifiableObjectUtils.getUids( selectionTreeManager.getSelectedOrganisationUnits() ) );

        HttpServletResponse response = ServletActionContext.getResponse();

        DataExportParams params = dataValueSetService.getFromUrl( selectedDataSets, null, null,
            getMediumDate( startDate ), getMediumDate( endDate ), orgUnits, true, null, false, null, null, null, idSchemes );

        if ( FORMAT_CSV.equals( exportFormat ) )
        {
            ContextUtils.configureResponse( response, CONTENT_TYPE_CSV, true, getFileName( EXTENSION_CSV_ZIP ), true );

            dataValueSetService.writeDataValueSetCsv( params, new OutputStreamWriter( getZipOut( response, getFileName( EXTENSION_CSV ) ) ) );
        }
        else if ( FORMAT_JSON.equals( exportFormat ) )
        {
            ContextUtils.configureResponse( response, CONTENT_TYPE_JSON, true, getFileName( EXTENSION_JSON_ZIP ), true );

            dataValueSetService.writeDataValueSetJson( params, getZipOut( response, getFileName( EXTENSION_JSON ) ) );
        }
        else
        {
            ContextUtils.configureResponse( response, CONTENT_TYPE_XML, true, getFileName( EXTENSION_XML_ZIP ), true );

            dataValueSetService.writeDataValueSetXml( params, getZipOut( response, getFileName( EXTENSION_XML ) ) );
        }

        return SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String getFileName( String extension )
    {
        String fileName = FILE_PREFIX + FILE_SEPARATOR + startDate + FILE_SEPARATOR + endDate;

        if ( selectionTreeManager.getSelectedOrganisationUnits().size() == 1 )
        {
            fileName += FILE_SEPARATOR + filenameEncode( selectionTreeManager.getSelectedOrganisationUnits().iterator().next().getShortName() );
        }

        return fileName + extension;
    }
}
