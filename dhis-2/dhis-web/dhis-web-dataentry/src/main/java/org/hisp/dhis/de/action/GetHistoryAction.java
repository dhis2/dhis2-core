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
package org.hisp.dhis.de.action;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataelementhistory.DataElementHistory;
import org.hisp.dhis.dataelementhistory.HistoryRetriever;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditQueryParams;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Torgeir Lorange Ostby
 * @author Halvdan Hoem Grelland
 */
public class GetHistoryAction
    implements Action
{
    private static final int HISTORY_LENGTH = 13;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private HistoryRetriever historyRetriever;

    public void setHistoryRetriever( HistoryRetriever historyRetriever )
    {
        this.historyRetriever = historyRetriever;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private DataValueService dataValueService;

    public void setDataValueService( DataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    private CategoryService categoryService;

    public void setCategoryService( CategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private DataValueAuditService dataValueAuditService;

    public void setDataValueAuditService( DataValueAuditService dataValueAuditService )
    {
        this.dataValueAuditService = dataValueAuditService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private FileResourceService fileResourceService;

    public void setFileResourceService( FileResourceService fileResourceService )
    {
        this.fileResourceService = fileResourceService;
    }

    @Autowired
    private InputUtils inputUtils;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String dataElementId;

    public String getDataElementId()
    {
        return dataElementId;
    }

    public void setDataElementId( String dataElementId )
    {
        this.dataElementId = dataElementId;
    }

    private String optionComboId;

    public String getOptionComboId()
    {
        return optionComboId;
    }

    public void setOptionComboId( String optionComboId )
    {
        this.optionComboId = optionComboId;
    }

    private String periodId;

    public String getPeriodId()
    {
        return periodId;
    }

    public void setPeriodId( String periodId )
    {
        this.periodId = periodId;
    }

    private String organisationUnitId;

    public void setOrganisationUnitId( String organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }

    private String cc;

    public void setCc( String cc )
    {
        this.cc = cc;
    }

    private String cp;

    public void setCp( String cp )
    {
        this.cp = cp;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private DataElementHistory dataElementHistory;

    public DataElementHistory getDataElementHistory()
    {
        return dataElementHistory;
    }

    private boolean historyInvalid;

    public boolean isHistoryInvalid()
    {
        return historyInvalid;
    }

    private boolean minMaxInvalid;

    public boolean isMinMaxInvalid()
    {
        return minMaxInvalid;
    }

    private DataValue dataValue;

    public DataValue getDataValue()
    {
        return dataValue;
    }

    private Collection<DataValueAudit> dataValueAudits;

    public Collection<DataValueAudit> getDataValueAudits()
    {
        return dataValueAudits;
    }

    private String storedBy;

    public String getStoredBy()
    {
        return storedBy;
    }

    private OptionSet commentOptionSet;

    public OptionSet getCommentOptionSet()
    {
        return commentOptionSet;
    }

    private Map<String, String> fileNames;

    public Map<String, String> getFileNames()
    {
        return fileNames;
    }

    private String attributeOptionComboId;

    public String getAttributeOptionComboId()
    {
        return attributeOptionComboId;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        DataElement dataElement = dataElementService.getDataElement( dataElementId );

        CategoryOptionCombo categoryOptionCombo = categoryService.getCategoryOptionCombo( optionComboId );

        if ( categoryOptionCombo == null )
        {
            categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        }

        if ( dataElement == null )
        {
            throw new IllegalArgumentException( "DataElement doesn't exist: " + dataElementId );
        }

        Period period = PeriodType.getPeriodFromIsoString( periodId );

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitId );

        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( cc, cp, false );

        dataElementHistory = historyRetriever.getHistory( dataElement, categoryOptionCombo, attributeOptionCombo,
            organisationUnit, period, HISTORY_LENGTH );

        dataValueAudits = dataValueAuditService.getDataValueAudits( new DataValueAuditQueryParams()
            .setDataElements( List.of( dataElement ) )
            .setPeriods( List.of( period ) )
            .setOrgUnits( List.of( organisationUnit ) )
            .setCategoryOptionCombo( categoryOptionCombo )
            .setAttributeOptionCombo( attributeOptionCombo ) );

        dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo,
            attributeOptionCombo );

        if ( dataValue != null )
        {
            User credentials = userService.getUserByUsername( dataValue.getStoredBy() );
            storedBy = credentials != null ? credentials.getName() : dataValue.getStoredBy();
        }

        if ( dataElement.isFileType() )
        {
            fileNames = new HashMap<>();
            dataValueAudits.removeIf( audit -> fileResourceService.getFileResource( audit.getValue() ) == null );
            dataValueAudits.stream()
                .filter( audit -> audit != null )
                .map( audit -> fileResourceService.getFileResource( audit.getValue() ) )
                .forEach( fr -> fileNames.put( fr.getUid(), fr.getName() ) );
        }

        historyInvalid = dataElementHistory == null;

        minMaxInvalid = !dataElement.getValueType().isNumeric();

        commentOptionSet = dataElement.getCommentOptionSet();

        attributeOptionComboId = attributeOptionCombo.getUid();

        return SUCCESS;
    }
}
