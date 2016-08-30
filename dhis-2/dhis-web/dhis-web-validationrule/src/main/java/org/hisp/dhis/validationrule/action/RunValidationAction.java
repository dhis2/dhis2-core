package org.hisp.dhis.validationrule.action;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.util.SessionUtils;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.ValidationRuleService;
import org.hisp.dhis.validation.comparator.ValidationResultComparator;

import com.opensymphony.xwork2.Action;

/**
 * @author Margrethe Store
 * @author Lars Helge Overland
 * @version $Id: RunValidationAction.java 6059 2008-10-28 15:15:34Z larshelg $
 */
public class RunValidationAction
    implements Action
{
    private static final Log log = LogFactory.getLog( RunValidationAction.class );

    private static final String KEY_VALIDATIONRESULT = "validationResult";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ValidationRuleService validationRuleService;

    public void setValidationRuleService( ValidationRuleService validationRuleService )
    {
        this.validationRuleService = validationRuleService;
    }

    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private DataElementCategoryService dataElementCategoryService;

    public void setDataElementCategoryService( DataElementCategoryService dataElementCategoryService )
    {
        this.dataElementCategoryService = dataElementCategoryService;
    }

    // -------------------------------------------------------------------------
    // Input/output
    // -------------------------------------------------------------------------

    private String organisationUnitId;

    public void setOrganisationUnitId( String organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }

    private String startDate;

    public String getStartDate()
    {
        return startDate;
    }

    public void setStartDate( String startDate )
    {
        this.startDate = startDate;
    }

    private String endDate;

    public String getEndDate()
    {
        return endDate;
    }

    public void setEndDate( String endDate )
    {
        this.endDate = endDate;
    }

    private Integer attributeOptionComboId;

    public void setAttributeOptionComboId( Integer attributeOptionComboId )
    {
        this.attributeOptionComboId = attributeOptionComboId;
    }

    private Integer validationRuleGroupId;

    public void setValidationRuleGroupId( Integer validationRuleGroupId )
    {
        this.validationRuleGroupId = validationRuleGroupId;
    }

    private boolean sendAlerts;

    public void setSendAlerts( boolean sendAlerts )
    {
        this.sendAlerts = sendAlerts;
    }

    private List<ValidationResult> validationResults = new ArrayList<>();

    public List<ValidationResult> getValidationResults()
    {
        return validationResults;
    }

    private boolean maxExceeded;

    public boolean isMaxExceeded()
    {
        return maxExceeded;
    }

    private boolean showAttributeCombos;

    public boolean isShowAttributeCombos()
    {
        return showAttributeCombos;
    }

    private OrganisationUnit organisationUnit;

    public OrganisationUnit getOrganisationUnit()
    {
        return organisationUnit;
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitId );

        Collection<OrganisationUnit> organisationUnits = organisationUnitService.getOrganisationUnitWithChildren( organisationUnit.getId() );

        ValidationRuleGroup group = validationRuleGroupId == -1 ? null : validationRuleService.getValidationRuleGroup( validationRuleGroupId );

        DataElementCategoryOptionCombo attributeOptionCombo = attributeOptionComboId == null || attributeOptionComboId == -1 ? null : dataElementCategoryService.getDataElementCategoryOptionCombo( attributeOptionComboId );

        log.info( "Validating data for " + ( group == null ? "all rules" : "group: " + group.getName() ) );

        validationResults = new ArrayList<>( validationRuleService.validate( format
                .parseDate( startDate ), format.parseDate( endDate ), organisationUnits, attributeOptionCombo, group, sendAlerts, format ) );

        maxExceeded = validationResults.size() > ValidationRuleService.MAX_INTERACTIVE_ALERTS;

        Collections.sort( validationResults, new ValidationResultComparator() );

        SessionUtils.setSessionVar( KEY_VALIDATIONRESULT, validationResults );

        computeShowAttributeCombos();

        log.info( "Validation done" );

        return SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void computeShowAttributeCombos()
    {
        showAttributeCombos = false;

        for ( ValidationResult result : validationResults )
        {
            if ( !result.getAttributeOptionCombo().isDefault() )
            {
                showAttributeCombos = true;
                break;
            }
        }
    }
}
