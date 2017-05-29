package org.hisp.dhis.dxf2.utils;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

/**
 * @author Lars Helge Overland
 */
public class InputUtils
{
    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    /**
     * Validates and retrieves the attribute option combo. 409 conflict as
     * status code along with a textual message will be set on the response in
     * case of invalid input.
     *
     * @param cc the category combo identifier.
     * @param cp the category and option query string.
     * @param skipFallback whether to skip fallback to default option combo if
     *        attribute option combo is not found.
     * @return the attribute option combo identified from the given input, or
     *         null. if the input was invalid.
     */
    public DataElementCategoryOptionCombo getAttributeOptionCombo( String cc, String cp, boolean skipFallback )
    {
        Set<String> opts = TextUtils.splitToArray( cp, TextUtils.SEMICOLON );

        // ---------------------------------------------------------------------
        // Attribute category combo validation
        // ---------------------------------------------------------------------

        if ( (cc == null && opts != null || (cc != null && opts == null)) )
        {
            throw new IllegalQueryException( "Both or none of category combination and category options must be present" );
        }

        DataElementCategoryCombo categoryCombo = null;

        if ( cc != null && (categoryCombo = idObjectManager.get( DataElementCategoryCombo.class, cc )) == null )
        {
            throw new IllegalQueryException( "Illegal category combo identifier: " + cc );
        }

        if ( categoryCombo == null && opts == null )
        {
            if ( skipFallback )
            {
                return null;
            }

            categoryCombo = categoryService.getDefaultDataElementCategoryCombo();
        }

        return getAttributeOptionCombo( categoryCombo, cp, null, IdScheme.UID );
    }

    /**
     * Validates and retrieves the attribute option combo. 409 conflict as
     * status code along with a textual message will be set on the response in
     * case of invalid input.
     *
     * @param categoryCombo the category combo.
     * @param cp the category option query string.
     * @param attributeOptionCombo the explicit attribute option combo identifier.
     * @return the attribute option combo identified from the given input, or
     *         null if the input was invalid.
     */
    public DataElementCategoryOptionCombo getAttributeOptionCombo( DataElementCategoryCombo categoryCombo, String cp, String attributeOptionCombo, IdScheme idScheme )
    {
        Set<String> opts = TextUtils.splitToArray( cp, TextUtils.SEMICOLON );

        return getAttributeOptionCombo( categoryCombo, opts, attributeOptionCombo, idScheme );
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
    public DataElementCategoryOptionCombo getAttributeOptionCombo( DataElementCategoryCombo categoryCombo, Set<String> opts, IdScheme idScheme )
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
    public DataElementCategoryOptionCombo getAttributeOptionCombo( DataElementCategoryCombo categoryCombo, Set<String> opts, String attributeOptionCombo, IdScheme idScheme )
    {
        if ( categoryCombo == null )
        {
            throw new IllegalQueryException( "Illegal category combo" );
        }

        // ---------------------------------------------------------------------
        // Attribute category options validation
        // ---------------------------------------------------------------------

        DataElementCategoryOptionCombo attrOptCombo = null;

        if ( opts != null )
        {
            Set<DataElementCategoryOption> categoryOptions = new HashSet<>();

            for ( String uid : opts )
            {
                DataElementCategoryOption categoryOption = idObjectManager.getObject( DataElementCategoryOption.class, idScheme, uid );

                if ( categoryOption == null )
                {
                    throw new IllegalQueryException( "Illegal category option identifier: " + uid );
                }

                categoryOptions.add( categoryOption );
            }

            attrOptCombo = categoryService.getDataElementCategoryOptionCombo( categoryCombo, categoryOptions );

            if ( attrOptCombo == null )
            {
                throw new IllegalQueryException( "Attribute option combo does not exist for given category combo and category options" );
            }
        }
        else if ( attributeOptionCombo != null )
        {
            attrOptCombo = categoryService.getDataElementCategoryOptionCombo( attributeOptionCombo );
        }

        // ---------------------------------------------------------------------
        // Fall back to default category option combination
        // ---------------------------------------------------------------------

        if ( attrOptCombo == null )
        {
            attrOptCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
        }

        if ( attrOptCombo == null )
        {
            throw new IllegalQueryException( "Default attribute option combo does not exist" );
        }

        return attrOptCombo;
    }
}
