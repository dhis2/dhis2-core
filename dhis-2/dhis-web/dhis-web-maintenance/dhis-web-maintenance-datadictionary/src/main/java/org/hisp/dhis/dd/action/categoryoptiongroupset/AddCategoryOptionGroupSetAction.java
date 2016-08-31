package org.hisp.dhis.dd.action.categoryoptiongroupset;

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
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Chau Thu Tran
 */
public class AddCategoryOptionGroupSetAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String description;

    public void setDescription( String description )
    {
        this.description = description;
    }

    private Boolean dataDimension;

    public void setDataDimension( Boolean dataDimension )
    {
        this.dataDimension = dataDimension;
    }
    
    private String dataDimensionType;

    public void setDataDimensionType( String dataDimensionType )
    {
        this.dataDimensionType = dataDimensionType;
    }

    private Set<String> cogSelected = new HashSet<>();

    public void setCogSelected( Set<String> cogSelected )
    {
        this.cogSelected = cogSelected;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        CategoryOptionGroupSet categoryOptionGroupSet = new CategoryOptionGroupSet( StringUtils.trimToNull( name ) );
        categoryOptionGroupSet.setDescription( StringUtils.trimToNull( description ) );
        categoryOptionGroupSet.setDataDimension( dataDimension );
        categoryOptionGroupSet.setDataDimensionType( DataDimensionType.valueOf( dataDimensionType ) );

        List<CategoryOptionGroup> members = new ArrayList<>();

        for ( String id : cogSelected )
        {
            members.add( dataElementCategoryService.getCategoryOptionGroup( id ) );
        }

        categoryOptionGroupSet.setMembers( members );

        dataElementCategoryService.saveCategoryOptionGroupSet( categoryOptionGroupSet );

        return SUCCESS;
    }

}
