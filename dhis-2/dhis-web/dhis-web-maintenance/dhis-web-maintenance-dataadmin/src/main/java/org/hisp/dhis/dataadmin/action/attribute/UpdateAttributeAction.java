package org.hisp.dhis.dataadmin.action.attribute;

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
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class UpdateAttributeAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private OptionService optionService;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String code;

    public void setCode( String code )
    {
        this.code = code;
    }

    private ValueType valueType;

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    private Boolean mandatory = false;

    public void setMandatory( Boolean mandatory )
    {
        this.mandatory = mandatory;
    }

    private Boolean unique = false;

    public void setUnique( Boolean unique )
    {
        this.unique = unique;
    }

    private Boolean dataElementAttribute = false;

    public void setDataElementAttribute( Boolean dataElementAttribute )
    {
        this.dataElementAttribute = dataElementAttribute;
    }

    private Boolean dataElementGroupAttribute = false;

    public void setDataElementGroupAttribute( Boolean dataElementGroupAttribute )
    {
        this.dataElementGroupAttribute = dataElementGroupAttribute;
    }

    private Boolean indicatorAttribute = false;

    public void setIndicatorAttribute( Boolean indicatorAttribute )
    {
        this.indicatorAttribute = indicatorAttribute;
    }

    private Boolean indicatorGroupAttribute = false;

    public void setIndicatorGroupAttribute( Boolean indicatorGroupAttribute )
    {
        this.indicatorGroupAttribute = indicatorGroupAttribute;
    }

    private Boolean dataSetAttribute = false;

    public void setDataSetAttribute( Boolean dataSetAttribute )
    {
        this.dataSetAttribute = dataSetAttribute;
    }

    private Boolean organisationUnitAttribute = false;

    public void setOrganisationUnitAttribute( Boolean organisationUnitAttribute )
    {
        this.organisationUnitAttribute = organisationUnitAttribute;
    }

    private Boolean organisationUnitGroupAttribute = false;

    public void setOrganisationUnitGroupAttribute( Boolean organisationUnitGroupAttribute )
    {
        this.organisationUnitGroupAttribute = organisationUnitGroupAttribute;
    }

    private Boolean organisationUnitGroupSetAttribute = false;

    public void setOrganisationUnitGroupSetAttribute( Boolean organisationUnitGroupSetAttribute )
    {
        this.organisationUnitGroupSetAttribute = organisationUnitGroupSetAttribute;
    }

    private Boolean userAttribute = false;

    public void setUserAttribute( Boolean userAttribute )
    {
        this.userAttribute = userAttribute;
    }

    private Boolean userGroupAttribute = false;

    public void setUserGroupAttribute( Boolean userGroupAttribute )
    {
        this.userGroupAttribute = userGroupAttribute;
    }

    private boolean programAttribute;

    public void setProgramAttribute( boolean programAttribute )
    {
        this.programAttribute = programAttribute;
    }

    private boolean programStageAttribute;

    public void setProgramStageAttribute( boolean programStageAttribute )
    {
        this.programStageAttribute = programStageAttribute;
    }

    private boolean trackedEntityAttribute;

    public void setTrackedEntityAttribute( boolean trackedEntityAttribute )
    {
        this.trackedEntityAttribute = trackedEntityAttribute;
    }

    private boolean trackedEntityAttributeAttribute;

    public void setTrackedEntityAttributeAttribute( boolean trackedEntityAttributeAttribute )
    {
        this.trackedEntityAttributeAttribute = trackedEntityAttributeAttribute;
    }

    private boolean categoryOptionAttribute;

    public void setCategoryOptionAttribute( boolean categoryOptionAttribute )
    {
        this.categoryOptionAttribute = categoryOptionAttribute;
    }

    private boolean categoryOptionGroupAttribute;

    public void setCategoryOptionGroupAttribute( boolean categoryOptionGroupAttribute )
    {
        this.categoryOptionGroupAttribute = categoryOptionGroupAttribute;
    }

    private boolean documentAttribute;

    public void setDocumentAttribute( boolean documentAttribute )
    {
        this.documentAttribute = documentAttribute;
    }

    private boolean optionAttribute;

    public void setOptionAttribute( boolean optionAttribute )
    {
        this.optionAttribute = optionAttribute;
    }

    private boolean optionSetAttribute;

    public void setOptionSetAttribute( boolean optionSetAttribute )
    {
        this.optionSetAttribute = optionSetAttribute;
    }

    private String optionSetUid;

    public void setOptionSetUid( String optionSetUid )
    {
        this.optionSetUid = optionSetUid;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        Attribute attribute = attributeService.getAttribute( id );

        if ( attribute != null )
        {
            OptionSet optionSet = optionService.getOptionSet( optionSetUid );
            valueType = optionSet != null && optionSet.getValueType() != null ? optionSet.getValueType() : valueType;

            attribute.setName( StringUtils.trimToNull( name ) );
            attribute.setCode( StringUtils.trimToNull( code ) );
            attribute.setValueType( valueType );
            attribute.setOptionSet( optionSet );
            attribute.setMandatory( mandatory );
            attribute.setUnique( unique );
            attribute.setDataElementAttribute( dataElementAttribute );
            attribute.setDataElementGroupAttribute( dataElementGroupAttribute );
            attribute.setIndicatorAttribute( indicatorAttribute );
            attribute.setIndicatorGroupAttribute( indicatorGroupAttribute );
            attribute.setDataSetAttribute( dataSetAttribute );
            attribute.setOrganisationUnitAttribute( organisationUnitAttribute );
            attribute.setOrganisationUnitGroupAttribute( organisationUnitGroupAttribute );
            attribute.setOrganisationUnitGroupSetAttribute( organisationUnitGroupSetAttribute );
            attribute.setUserAttribute( userAttribute );
            attribute.setUserGroupAttribute( userGroupAttribute );
            attribute.setProgramAttribute( programAttribute );
            attribute.setProgramStageAttribute( programStageAttribute );
            attribute.setTrackedEntityAttribute( trackedEntityAttribute );
            attribute.setTrackedEntityAttributeAttribute( trackedEntityAttributeAttribute );
            attribute.setCategoryOptionAttribute( categoryOptionAttribute );
            attribute.setCategoryOptionGroupAttribute( categoryOptionGroupAttribute );
            attribute.setDocumentAttribute( documentAttribute );
            attribute.setOptionAttribute( optionAttribute );
            attribute.setOptionSetAttribute( optionSetAttribute );

            attributeService.updateAttribute( attribute );

            return SUCCESS;
        }

        return ERROR;
    }
}
