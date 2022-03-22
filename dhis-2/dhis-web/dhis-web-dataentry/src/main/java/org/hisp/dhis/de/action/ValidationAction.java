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

import java.util.*;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataanalysis.DataAnalysisService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.validation.ValidationAnalysisParams;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import com.opensymphony.xwork2.Action;

/**
 * @author Margrethe Store
 * @author Lars Helge Overland
 */
@Slf4j
public class ValidationAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private DataAnalysisService minMaxOutlierAnalysisService;

    public void setMinMaxOutlierAnalysisService( DataAnalysisService minMaxOutlierAnalysisService )
    {
        this.minMaxOutlierAnalysisService = minMaxOutlierAnalysisService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private CategoryService dataElementCategoryService;

    public void setCategoryService( CategoryService dataElementCategoryService )
    {
        this.dataElementCategoryService = dataElementCategoryService;
    }

    private ValidationService validationService;

    public void setValidationService( ValidationService validationService )
    {
        this.validationService = validationService;
    }

    @Autowired
    private InputUtils inputUtils;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String ds;

    public void setDs( String ds )
    {
        this.ds = ds;
    }

    private String pe;

    public void setPe( String pe )
    {
        this.pe = pe;
    }

    private String ou;

    public void setOu( String ou )
    {
        this.ou = ou;
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

    private boolean multiOu;

    public boolean isMultiOu()
    {
        return multiOu;
    }

    public void setMultiOu( boolean multiOu )
    {
        this.multiOu = multiOu;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private Map<String, List<ValidationResult>> validationResults = new TreeMap<>();

    public Map<String, List<ValidationResult>> getValidationResults()
    {
        return validationResults;
    }

    private Map<String, List<DeflatedDataValue>> dataValues = new TreeMap<>();

    public Map<String, List<DeflatedDataValue>> getDataValues()
    {
        return dataValues;
    }

    private Map<String, List<DataElementOperand>> commentViolations = new TreeMap<>();

    public Map<String, List<DataElementOperand>> getCommentViolations()
    {
        return commentViolations;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( ou );

        DataSet dataSet = dataSetService.getDataSet( ds );

        Period selectedPeriod = PeriodType.getPeriodFromIsoString( pe );

        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( cc, cp, false );

        if ( attributeOptionCombo == null )
        {
            attributeOptionCombo = dataElementCategoryService.getDefaultCategoryOptionCombo();
        }

        if ( selectedPeriod == null || orgUnit == null || (multiOu && !orgUnit.hasChild()) )
        {
            return SUCCESS;
        }

        Period period = periodService.getPeriod( selectedPeriod.getStartDate(), selectedPeriod.getEndDate(),
            selectedPeriod.getPeriodType() );

        if ( period == null )
        {
            return SUCCESS;
        }

        List<OrganisationUnit> organisationUnits = new ArrayList<>();

        if ( !multiOu )
        {
            organisationUnits.add( orgUnit );
        }
        else
        {
            organisationUnits.addAll( orgUnit.getChildren() );
        }

        Collections.sort( organisationUnits );

        Date from = new DateTime( period.getStartDate() ).minusYears( 2 ).toDate();

        for ( OrganisationUnit organisationUnit : organisationUnits )
        {
            List<DeflatedDataValue> values = new ArrayList<>(
                minMaxOutlierAnalysisService.analyse( Sets.newHashSet( organisationUnit ),
                    dataSet.getDataElements(), Sets.newHashSet( period ), null, from ) );

            if ( !values.isEmpty() )
            {
                dataValues.put( organisationUnit.getUid(), values );
            }

            ValidationAnalysisParams params = validationService.newParamsBuilder( dataSet, organisationUnit, period )
                .withAttributeOptionCombo( attributeOptionCombo )
                .build();

            List<ValidationResult> results = new ArrayList<>( validationService.validationAnalysis( params,
                NoopJobProgress.INSTANCE ) );

            if ( !results.isEmpty() )
            {
                validationResults.put( organisationUnit.getUid(), results );
            }

            List<DataElementOperand> violations = validationService.validateRequiredComments( dataSet, period,
                organisationUnit, attributeOptionCombo );

            log.info(
                "Validation done for data set: '{}', period: '{}', org unit: '{}', validation rule count: {}, violations found: {}",
                dataSet.getUid(), period.getIsoDate(), organisationUnit.getUid(), params.getValidationRules().size(),
                violations.size() );

            if ( !violations.isEmpty() )
            {
                commentViolations.put( organisationUnit.getUid(), violations );
            }
        }

        return dataValues.isEmpty() && validationResults.isEmpty() && commentViolations.isEmpty() ? SUCCESS : INPUT;
    }
}
