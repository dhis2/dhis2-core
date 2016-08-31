package org.hisp.dhis.dd.action.categoryoptiongroup;

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

import com.opensymphony.xwork2.Action;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.system.util.AttributeUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Chau Thu Tran
 */
public class AddCategoryOptionGroupAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    @Autowired
    private AttributeService attributeService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String shortName;

    public void setShortName( String shortName )
    {
        this.shortName = shortName;
    }

    private String code;

    public void setCode( String code )
    {
        this.code = code;
    }

    private String dataDimensionType;

    public void setDataDimensionType( String dataDimensionType )
    {
        this.dataDimensionType = dataDimensionType;
    }

    private Set<String> coSelected = new HashSet<>();

    public void setCoSelected( Set<String> coSelected )
    {
        this.coSelected = coSelected;
    }

    private List<String> jsonAttributeValues;

    public void setJsonAttributeValues( List<String> jsonAttributeValues )
    {
        this.jsonAttributeValues = jsonAttributeValues;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        CategoryOptionGroup categoryOptionGroup = new CategoryOptionGroup( StringUtils.trimToNull( name ) );
        categoryOptionGroup.setShortName( StringUtils.trimToNull( shortName ) );
        categoryOptionGroup.setCode( StringUtils.trimToNull( code ) );
        categoryOptionGroup.setDataDimensionType( DataDimensionType.valueOf( dataDimensionType ) );

        for ( String id : coSelected )
        {
            categoryOptionGroup.addCategoryOption( dataElementCategoryService.getDataElementCategoryOption( id ) );
        }

        if ( jsonAttributeValues != null )
        {
            AttributeUtils.updateAttributeValuesFromJson( categoryOptionGroup.getAttributeValues(), jsonAttributeValues,
                attributeService );
        }

        dataElementCategoryService.saveCategoryOptionGroup( categoryOptionGroup );

        return SUCCESS;
    }

}
