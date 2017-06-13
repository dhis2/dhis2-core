package org.hisp.dhis.dataelement;

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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.IdentifiableProperty;

/**
 * CategoryComboMap is used to lookup categoryoptioncombos by identifiers of the
 * categoryoptions. Identifiers must be trimmed of whitespace and be
 * concatenated in the order given by the categories property in object of this
 * class.
 * 
 * @author bobj
 */
public class CategoryComboMap
{
    private final IdentifiableProperty idScheme;

    private final List<DataElementCategory> categories;

    private DataElementCategoryCombo categoryCombo;

    private Map<String, DataElementCategoryOptionCombo> ccMap;

    public List<DataElementCategory> getCategories()
    {
        return categories;
    }

    public IdentifiableProperty getIdScheme()
    {
        return idScheme;
    }

    public DataElementCategoryCombo getCategoryCombo()
    {
        return categoryCombo;
    }

    public class CategoryComboMapException
        extends Exception
    {
        public CategoryComboMapException( String msg )
        {
            super( msg );
        }
    }

    public CategoryComboMap( DataElementCategoryCombo cc, IdentifiableProperty idScheme )
        throws CategoryComboMapException
    {
        this.categoryCombo = cc;
        this.idScheme = idScheme;
        ccMap = new HashMap<>();

        categories = categoryCombo.getCategories();

        Collection<DataElementCategoryOptionCombo> optionCombos = categoryCombo.getOptionCombos();

        for ( DataElementCategoryOptionCombo optionCombo : optionCombos )
        {
            String compositeIdentifier = "";

            for ( DataElementCategory category : categories )
            {
                DataElementCategoryOption catopt = category.getCategoryOption( optionCombo );
                
                if ( catopt == null )
                {
                    throw new CategoryComboMapException( "No categoryOption in " + category.getName() + " matching "
                        + optionCombo.getName() );
                }
                else
                {
                    String identifier;

                    switch ( idScheme )
                    {
                        case UID:
                            identifier = catopt.getUid().trim();
                            break;
                        case CODE:
                            identifier = catopt.getCode().trim();
                            break;
                        case NAME:
                            identifier = catopt.getName().trim();
                            break;
                        default:
                            throw new CategoryComboMapException( "Unsupported ID Scheme: " + idScheme.toString() );
                    }

                    if ( identifier == null )
                    {
                        throw new CategoryComboMapException( "No " + idScheme.toString() + " identifier for CategoryOption: " + catopt.getName() );
                    }

                    compositeIdentifier += "\"" + identifier + "\"";
                }
            }
            
            this.ccMap.put( compositeIdentifier, optionCombo );
        }
    }

    /**
     * Look up the categoryoptioncombo based on a composite identifier
     * 
     * @param compositeIdentifier
     * @return
     */
    public DataElementCategoryOptionCombo getCategoryOptionCombo( String compositeIdentifier )
    {
        return ccMap.get( compositeIdentifier );
    }

    /**
     * Create a composite identifier from a list of identifiers
     * 
     * Note: identifiers must be in same order as list of categories in the map
     * 
     * @param identifiers
     * @return
     */
    public String getKey( List<String> identifiers )
    {
        String key = "";
        
        for ( String identifier : identifiers )
        {
            key += "\"" + identifier + "\"";
        }
        
        return key;
    }
    
    public String toString()
    {
        return "CatComboMap: catcombo=" + categoryCombo.getName() + " map:" + ccMap.toString();
    }
}
