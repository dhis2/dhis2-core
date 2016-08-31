package org.hisp.dhis.dd.action.categorycombo;

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
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Abyot Asalefew
 */
public class UpdateDataElementCategoryComboAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementCategoryService dataElementCategoryService;

    public void setDataElementCategoryService( DataElementCategoryService dataElementCategoryService )
    {
        this.dataElementCategoryService = dataElementCategoryService;
    }

    // -------------------------------------------------------------------------
    // Input
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

    private boolean skipTotal;

    public void setSkipTotal( boolean skipTotal )
    {
        this.skipTotal = skipTotal;
    }

    private List<String> caSelected = new ArrayList<>();

    public void setCaSelected( List<String> caSelected )
    {
        this.caSelected = caSelected;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        DataElementCategoryCombo categoryCombo = dataElementCategoryService.getDataElementCategoryCombo( id );

        categoryCombo.setName( StringUtils.trimToNull( name ) );
        categoryCombo.setCode( StringUtils.trimToNull( code ) );
        categoryCombo.setSkipTotal( skipTotal );

        List<DataElementCategory> updatedCategories = new ArrayList<>();

        for ( String id : caSelected )
        {
            DataElementCategory dataElementCategory = dataElementCategoryService.getDataElementCategory( id );
            updatedCategories.add( dataElementCategory );
        }

        categoryCombo.setCategories( updatedCategories );

        dataElementCategoryService.updateDataElementCategoryCombo( categoryCombo );

        dataElementCategoryService.updateOptionCombos( categoryCombo );

        return SUCCESS;
    }
}
