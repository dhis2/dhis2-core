package org.hisp.dhis.dxf2.utils;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.SimpleCacheBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Lars Helge Overland
 */
public class InputUtils
{
    private static Cache<Integer>  ATTR_OPTION_COMBO_ID_CACHE = new SimpleCacheBuilder<Integer>()
        .expireAfterWrite( 3, TimeUnit.HOURS )
        .withInitialCapacity( 1000 )
        .forceInMemory()
        .withMaximumSize( SystemUtils.isTestRun() ? 0 : 10000 )
        .build();

    private final CategoryService categoryService;

    private final IdentifiableObjectManager idObjectManager;
    
    public InputUtils( CategoryService categoryService, IdentifiableObjectManager idObjectManager )
    {
        checkNotNull( categoryService );
        checkNotNull( idObjectManager );

        this.categoryService = categoryService;
        this.idObjectManager = idObjectManager;
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
     *         null. if the input was invalid.
     */
    public CategoryOptionCombo getAttributeOptionCombo( String cc, String cp, boolean skipFallback )
    {
        String cacheKey = TextUtils.joinHyphen( cc, cp, String.valueOf( skipFallback ) );

        Integer id = ATTR_OPTION_COMBO_ID_CACHE.getIfPresent( cacheKey ).orElse( null );

        if ( id != null )
        {
            return categoryService.getCategoryOptionCombo( id );
        }
        else
        {
            CategoryOptionCombo aoc = getAttributeOptionComboInternal( cc, cp, skipFallback );

            if ( aoc != null )
            {
                ATTR_OPTION_COMBO_ID_CACHE.put( cacheKey, aoc.getId() );
            }

            return aoc;
        }
    }

    private CategoryOptionCombo getAttributeOptionComboInternal( String cc, String cp, boolean skipFallback )
    {
        Set<String> opts = TextUtils.splitToArray( cp, TextUtils.SEMICOLON );

        // ---------------------------------------------------------------------
        // Attribute category combo validation
        // ---------------------------------------------------------------------

        if ( (cc == null && opts != null || (cc != null && opts == null)) )
        {
            throw new IllegalQueryException( "Both or none of category combination and category options must be present" );
        }

        CategoryCombo categoryCombo = null;

        if ( cc != null && (categoryCombo = idObjectManager.get( CategoryCombo.class, cc )) == null )
        {
            throw new IllegalQueryException( "Illegal category combo identifier: " + cc );
        }

        if ( categoryCombo == null )
        {
            if ( skipFallback )
            {
                return null;
            }

            categoryCombo = categoryService.getDefaultCategoryCombo();
        }

        return getAttributeOptionCombo( categoryCombo, opts, null, IdScheme.UID );
    }

    /**
     * Validates and retrieves the attribute option combo. 409 conflict as
     * status code along with a textual message will be set on the response in
     * case of invalid input.
     *
     * @param categoryCombo the category combo.
     * @param opts list of category option uid.
     * @return the attribute option combo identified from the given input, or
     *         null if the input was invalid.
     */
    public CategoryOptionCombo getAttributeOptionCombo( CategoryCombo categoryCombo, Set<String> opts, IdScheme idScheme )
    {
        return getAttributeOptionCombo( categoryCombo, opts, null, idScheme );
    }

    /**
     * Validates and retrieves the attribute option combo. 409 conflict as
     * status code along with a textual message will be set on the response in
     * case of invalid input.
     *
     * @param categoryCombo the category combo.
     * @param opts list of category option uid.
     * @param attributeOptionCombo the explicit attribute option combo
     *        identifier.
     * @return the attribute option combo identified from the given input, or
     *         null if the input was invalid.
     */
    public CategoryOptionCombo getAttributeOptionCombo( CategoryCombo categoryCombo, Set<String> opts, String attributeOptionCombo, IdScheme idScheme )
    {
        if ( categoryCombo == null )
        {
            throw new IllegalQueryException( "Illegal category combo" );
        }

        // ---------------------------------------------------------------------
        // Attribute category options validation
        // ---------------------------------------------------------------------

        CategoryOptionCombo attrOptCombo = null;

        if ( opts != null )
        {
            Set<CategoryOption> categoryOptions = new HashSet<>();

            for ( String uid : opts )
            {
                CategoryOption categoryOption = idObjectManager.getObject( CategoryOption.class, idScheme, uid );

                if ( categoryOption == null )
                {
                    throw new IllegalQueryException( "Illegal category option identifier: " + uid );
                }

                categoryOptions.add( categoryOption );
            }

            attrOptCombo = categoryService.getCategoryOptionCombo( categoryCombo, categoryOptions );

            if ( attrOptCombo == null )
            {
                throw new IllegalQueryException( "Attribute option combo does not exist for given category combo and category options" );
            }
        }
        else if ( attributeOptionCombo != null )
        {
            attrOptCombo = categoryService.getCategoryOptionCombo( attributeOptionCombo );
        }

        // ---------------------------------------------------------------------
        // Fall back to default category option combination
        // ---------------------------------------------------------------------

        if ( attrOptCombo == null )
        {
            attrOptCombo = categoryService.getDefaultCategoryOptionCombo();
        }

        if ( attrOptCombo == null )
        {
            throw new IllegalQueryException( "Default attribute option combo does not exist" );
        }

        return attrOptCombo;
    }

    /**
     * Checks if user is authorized to force data input.
     * Having just the authority is not enough. User has to explicitly ask for it.
     *
     * @param currentUser the user attempting to force data input
     * @param force request to force data input
     * @return true if authorized and requested for it, otherwise false
     */
    public boolean canForceDataInput( User currentUser, boolean force )
    {
        return force && currentUser.isSuper();
    }
}
