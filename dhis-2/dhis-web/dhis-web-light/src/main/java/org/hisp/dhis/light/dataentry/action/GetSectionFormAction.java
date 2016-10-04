package org.hisp.dhis.light.dataentry.action;

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
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.light.utils.FormUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class GetSectionFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private FormUtils formUtils;

    public void setFormUtils( FormUtils formUtils )
    {
        this.formUtils = formUtils;
    }

    public FormUtils getFormUtils()
    {
        return formUtils;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private Integer organisationUnitId;

    public void setOrganisationUnitId( Integer organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }

    public Integer getOrganisationUnitId()
    {
        return organisationUnitId;
    }

    private String isoPeriod;

    public String getIsoPeriod()
    {
        return isoPeriod;
    }

    public void setIsoPeriod( String isoPeriod )
    {
        this.isoPeriod = isoPeriod;
    }

    private Integer dataSetId;

    public void setDataSetId( Integer dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    public Integer getDataSetId()
    {
        return dataSetId;
    }

    private Integer sectionId;

    public void setSectionId( Integer sectionId )
    {
        this.sectionId = sectionId;
    }

    public Integer getSectionId()
    {
        return sectionId;
    }

    private DataSet dataSet;

    public DataSet getDataSet()
    {
        return dataSet;
    }

    private Map<String, String> dataValues = new HashMap<>();

    public Map<String, String> getDataValues()
    {
        return dataValues;
    }

    private Map<String, DeflatedDataValue> validationViolations = new HashMap<>();

    public Map<String, DeflatedDataValue> getValidationViolations()
    {
        return validationViolations;
    }

    private List<String> validationRuleViolations = new ArrayList<>();

    public List<String> getValidationRuleViolations()
    {
        return validationRuleViolations;
    }

    private Map<String, Boolean> greyedFields = new HashMap<>();

    public Map<String, Boolean> getGreyedFields()
    {
        return greyedFields;
    }

    // FIXME: Not in use, but seems to be referenced in html.
    private Map<String, String> typeViolations = new HashMap<>();

    public Map<String, String> getTypeViolations()
    {
        return typeViolations;
    }

    private String name;

    public String getName()
    {
        return name;
    }

    private List<DataElement> dataElements = new ArrayList<>();

    public List<DataElement> getDataElements()
    {
        return dataElements;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        Validate.notNull( organisationUnitId );
        Validate.notNull( isoPeriod );
        Validate.notNull( dataSetId );

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitId );

        Period period = periodService.getPeriod( isoPeriod );

        dataSet = dataSetService.getDataSet( dataSetId );

        dataValues = formUtils.getDataValueMap( organisationUnit, dataSet, period );

        validationRuleViolations = formUtils.getValidationRuleViolations( organisationUnit, dataSet, period );

        if ( dataSet.getFormType().isSection() )
        {
            setGreyedFields();
        }

        if ( sectionId != null )
        {
            for ( Section section : dataSet.getSections() )
            {
                if ( section.getId() == sectionId )
                {
                    name = section.getName();
                    dataElements = section.getDataElements();

                    break;
                }
            }
        }
        else
        {
            name = "Default";
            dataElements = new ArrayList<>( dataSet.getDataElements() );
            Collections.sort( dataElements );
        }

        validationViolations = formUtils.getValidationViolations( organisationUnit, dataElements, period );

        return SUCCESS;
    }

    private void setGreyedFields()
    {
        for ( Section section : dataSet.getSections() )
        {
            for ( DataElementOperand operand : section.getGreyedFields() )
            {
                greyedFields.put( operand.getDataElement().getId() + ":" + operand.getCategoryOptionCombo().getId(), true );
            }
        }
    }
}
