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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 * 
 * @version $ GetCategoryOptionGroupSetListAction.java Feb 12, 2014 11:27:01 PM
 *          $
 */
public class GetCategoryOptionGroupSetListAction
    extends ActionPagingSupport<CategoryOptionGroupSet>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String key;

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    private List<CategoryOptionGroupSet> categoryOptionGroupSets;

    public List<CategoryOptionGroupSet> getCategoryOptionGroupSets()
    {
        return categoryOptionGroupSets;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( isNotBlank( key ) ) // Filter on key only if set
        {
            this.paging = createPaging( dataElementCategoryService.getCategoryOptionGroupSetCountByName( key ) );

            categoryOptionGroupSets = dataElementCategoryService.getCategoryOptionGroupSetsBetweenByName( paging.getStartPos(),
                    paging.getPageSize(), key );
        }
        else
        {
            this.paging = createPaging( dataElementCategoryService.getCategoryOptionGroupSetCount() );

            categoryOptionGroupSets = dataElementCategoryService.getCategoryOptionGroupSetsBetween( paging.getStartPos(),
                    paging.getPageSize() );
        }

        Collections.sort( categoryOptionGroupSets, IdentifiableObjectNameComparator.INSTANCE );

        return SUCCESS;
    }

}
