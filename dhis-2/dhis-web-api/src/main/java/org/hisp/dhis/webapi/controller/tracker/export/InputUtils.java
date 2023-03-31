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
package org.hisp.dhis.webapi.controller.tracker.export;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component
public class InputUtils
{
    private final Cache<Long> attrOptionComboIdCache;

    private final CategoryService categoryService;

    private final IdentifiableObjectManager idObjectManager;

    public InputUtils( CategoryService categoryService, IdentifiableObjectManager idObjectManager,
        CacheProvider cacheProvider )
    {
        this.categoryService = categoryService;
        this.idObjectManager = idObjectManager;
        this.attrOptionComboIdCache = cacheProvider.createAttrOptionComboIdCache();
    }

    /**
     * Validates and retrieves the attribute option combo. 409 conflict as
     * status code along with a textual message will be set on the response in
     * case of invalid input. The response is cached.
     *
     * @param cc the category combo identifier.
     * @param cp the category and option query string.
     * @param skipFallback whether to skip fallback to default option combo if
     *        attribute option combo is not found.
     * @return the attribute option combo identified from the given input, or
     *         null if the input was invalid.
     */
    public CategoryOptionCombo getAttributeOptionCombo( String cc, String cp, boolean skipFallback )
    {
        Set<String> options = TextUtils.splitToSet( cp, TextUtils.SEMICOLON );

        return getAttributeOptionCombo( cc, options, skipFallback );
    }

    private CategoryOptionCombo getAttributeOptionCombo( String cc, Set<String> options, boolean skipFallback )
    {
        String cacheKey = TextUtils.joinHyphen( cc, TextUtils.joinHyphen( options ), String.valueOf( skipFallback ) );

        Long id = attrOptionComboIdCache.getIfPresent( cacheKey ).orElse( null );

        if ( id != null )
        {
            return categoryService.getCategoryOptionCombo( id );
        }
        else
        {
            CategoryOptionCombo aoc = getAttributeOptionComboInternal( cc, options, skipFallback );

            if ( aoc != null )
            {
                attrOptionComboIdCache.put( cacheKey, aoc.getId() );
            }

            return aoc;
        }
    }

    private CategoryOptionCombo getAttributeOptionComboInternal( String cc, Set<String> options, boolean skipFallback )
    {
        // ---------------------------------------------------------------------
        // Attribute category combo validation
        // ---------------------------------------------------------------------

        if ( (cc == null && options != null) || (cc != null && options == null) )
        {
            throw new IllegalQueryException( ErrorCode.E2040 );
        }

        CategoryCombo categoryCombo = null;

        if ( cc != null && (categoryCombo = idObjectManager.get( CategoryCombo.class, cc )) == null )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1110, cc ) );
        }

        if ( categoryCombo == null )
        {
            if ( skipFallback )
            {
                return null;
            }

            categoryCombo = categoryService.getDefaultCategoryCombo();
        }

        return getAttributeOptionCombo( categoryCombo, options );
    }

    private CategoryOptionCombo getAttributeOptionCombo( CategoryCombo categoryCombo, Set<String> options )
    {
        if ( categoryCombo == null )
        {
            throw new IllegalQueryException( "Illegal category combo" );
        }

        // ---------------------------------------------------------------------
        // Attribute category options validation
        // ---------------------------------------------------------------------

        CategoryOptionCombo attrOptCombo = null;

        if ( options != null )
        {
            Set<CategoryOption> categoryOptions = new HashSet<>();

            for ( String option : options )
            {
                CategoryOption categoryOption = idObjectManager.getObject( CategoryOption.class, IdScheme.UID, option );

                if ( categoryOption == null )
                {
                    throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1111, option ) );
                }

                categoryOptions.add( categoryOption );
            }

            attrOptCombo = categoryService.getCategoryOptionCombo( categoryCombo, categoryOptions );

            if ( attrOptCombo == null )
            {
                throw new IllegalQueryException( ErrorCode.E2041 );
            }
        }

        // ---------------------------------------------------------------------
        // Fall back to default category option combination
        // ---------------------------------------------------------------------

        if ( attrOptCombo == null )
        {
            attrOptCombo = categoryService.getDefaultCategoryOptionCombo();
        }

        return attrOptCombo;
    }
}
