package org.hisp.dhis.dxf2.adx;

/*
 * Copyright (c) 2015, UiO
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.xerces.util.XMLChar;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;

/**
 * @author bobj
 */
public class AdxDataSetMetadata
{
    // Lookup category options per cat option combo
    
    private final Map<Integer, Map<String, String>> categoryOptionMap;

    AdxDataSetMetadata( DataSet dataSet )
        throws AdxException
    {
        categoryOptionMap = new HashMap<>();

        Set<DataElementCategoryCombo> catCombos = new HashSet<>();

        catCombos.add( dataSet.getCategoryCombo() );
        
        for ( DataSetElement element : dataSet.getDataSetElements() )
        {
            catCombos.add( element.getResolvedCategoryCombo() );
        }

        for ( DataElementCategoryCombo categoryCombo : catCombos )
        {
            for ( DataElementCategoryOptionCombo catOptCombo : categoryCombo.getOptionCombos() )
            {
                addExplodedCategoryAttributes( catOptCombo );
            }
        }
    }

    private void addExplodedCategoryAttributes( DataElementCategoryOptionCombo coc )
        throws AdxException
    {
        Map<String, String> categoryAttributes = new HashMap<>();

        if ( !coc.isDefault() )
        {
            for ( DataElementCategory category : coc.getCategoryCombo().getCategories() )
            {
                String categoryCode = category.getCode();
                
                if ( categoryCode == null || !XMLChar.isValidName( categoryCode ) )
                {
                    throw new AdxException(
                        "Category code for " + category.getName() + " is missing or invalid: " + categoryCode );
                }

                String catOptCode = category.getCategoryOption( coc ).getCode();
                
                if ( catOptCode == null || catOptCode.isEmpty() )
                {
                    throw new AdxException(
                        "CategoryOption code for " + category.getCategoryOption( coc ).getName() + " is missing" );
                }

                categoryAttributes.put( categoryCode, catOptCode );
            }
        }

        categoryOptionMap.put( coc.getId(), categoryAttributes );
    }

    public Map<String, String> getExplodedCategoryAttributes( int cocId )
    {
        return this.categoryOptionMap.get( cocId );
    }
}
