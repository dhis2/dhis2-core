package org.hisp.dhis.dd.action.category;

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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;

import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.paging.ActionPagingSupport;

/**
 * @author Chau Thu Tran
 * @version GetDataElementCategoryOptionListAction.java 8:47:42 AM Feb 22, 2013 $
 */
public class GetDataElementCategoryOptionListAction
    extends ActionPagingSupport<DataElementCategoryOption>
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
    // Getters & Setters
    // -------------------------------------------------------------------------

    private List<DataElementCategoryOption> dataElementCategoryOptions;

    public List<DataElementCategoryOption> getDataElementCategoryOptions()
    {
        return dataElementCategoryOptions;
    }

    private DataElementCategoryOption defaultCategoryOption;

    public DataElementCategoryOption getDefaultCategoryOption()
    {
        return defaultCategoryOption;
    }

    private String key;

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        defaultCategoryOption = dataElementCategoryService.getDataElementCategoryOptionByName( DataElementCategoryOption.DEFAULT_NAME );

        if ( isNotBlank( key ) ) // Filter on key only if set
        {
            this.paging = createPaging( dataElementCategoryService.getDataElementCategoryOptionCountByName( key ) );

            dataElementCategoryOptions = dataElementCategoryService.getDataElementCategoryOptionsBetweenByName( key, paging.getStartPos(), paging.getPageSize() );
        }
        else
        {
            this.paging = createPaging( dataElementCategoryService.getDataElementCategoryOptionCount() );

            dataElementCategoryOptions = dataElementCategoryService.getDataElementCategoryOptionsBetween( paging.getStartPos(), paging.getPageSize() );
        }

        return SUCCESS;
    }

}
