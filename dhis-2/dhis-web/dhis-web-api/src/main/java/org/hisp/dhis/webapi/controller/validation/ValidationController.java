package org.hisp.dhis.webapi.controller.validation;

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

import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.validation.ValidationRuleService;
import org.hisp.dhis.validation.ValidationSummary;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = "/validation" )
public class ValidationController
{
    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementCategoryService categoryService;

    @RequestMapping( value = "/dataSet/{ds}", method = RequestMethod.GET )
    public String validate( @PathVariable String ds, @RequestParam String pe,
        @RequestParam String ou, @RequestParam( required = false ) String aoc,
        HttpServletResponse response, Model model ) throws WebMessageException
    {
        DataSet dataSet = dataSetService.getDataSet( ds );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set does not exist: " + ds ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Period does not exist: " + pe ) );
        }

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( ou );

        if ( orgUnit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit does not exist: " + ou ) );
        }

        DataElementCategoryOptionCombo attributeOptionCombo = categoryService.getDataElementCategoryOptionCombo( aoc );

        if ( attributeOptionCombo == null )
        {
            attributeOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
        }

        ValidationSummary summary = new ValidationSummary();

        summary.setValidationRuleViolations( new ArrayList<>( validationRuleService.validate( dataSet, period, orgUnit, attributeOptionCombo ) ) );
        summary.setCommentRequiredViolations( validationRuleService.validateRequiredComments( dataSet, period, orgUnit, attributeOptionCombo ) );

        model.addAttribute( "model", summary );

        return "summary";
    }
}
