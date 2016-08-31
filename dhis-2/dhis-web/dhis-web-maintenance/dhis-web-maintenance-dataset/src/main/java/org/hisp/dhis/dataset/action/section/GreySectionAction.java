package org.hisp.dhis.dataset.action.section;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionService;

import com.opensymphony.xwork2.Action;

public class GreySectionAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SectionService sectionService;

    public void setSectionService( SectionService sectionService )
    {
        this.sectionService = sectionService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    // -------------------------------------------------------------------------
    // Input & output
    // -------------------------------------------------------------------------

    private Integer sectionId;

    private Section section;

    private DataElementCategoryCombo categoryCombo;

    private Boolean sectionIsMultiDimensional = false;

    public Integer getSectionId()
    {
        return sectionId;
    }

    public void setSectionId( Integer sectionId )
    {
        this.sectionId = sectionId;
    }

    public Section getSection()
    {
        return section;
    }

    public void setSection( Section section )
    {
        this.section = section;
    }

    public void setCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        this.categoryCombo = categoryCombo;
    }

    public DataElementCategoryCombo getCategoryCombo()
    {
        return categoryCombo;
    }

    public Boolean getSectionIsMultiDimensional()
    {
        return sectionIsMultiDimensional;
    }

    private Map<Integer, Collection<DataElementCategoryOption>> optionsMap = new HashMap<>();

    public Map<Integer, Collection<DataElementCategoryOption>> getOptionsMap()
    {
        return optionsMap;
    }

    private Map<Integer, Collection<DataElementCategory>> orderedCategories = new HashMap<>();

    public Map<Integer, Collection<DataElementCategory>> getOrderedCategories()
    {
        return orderedCategories;
    }

    private Map<Integer, Collection<Integer>> colRepeat = new HashMap<>();

    public Map<Integer, Collection<Integer>> getColRepeat()
    {
        return colRepeat;
    }

    private List<DataElementCategoryOptionCombo> optionCombos = new ArrayList<>();

    public List<DataElementCategoryOptionCombo> getOptionCombos()
    {
        return optionCombos;
    }

    private List<DataElementCategoryCombo> orderedCategoryCombos = new ArrayList<>();

    public List<DataElementCategoryCombo> getOrderedCategoryCombos()
    {
        return orderedCategoryCombos;
    }

    private Map<String, Boolean> greyedFields = new HashMap<>();

    public Map<String, Boolean> getGreyedFields()
    {
        return greyedFields;
    }

    private Integer defaultOptionComboId;

    public Integer getDefaultOptionComboId()
    {
        return defaultOptionComboId;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------    

    @Override
    public String execute()
        throws Exception
    {
        section = sectionService.getSection( sectionId.intValue() );

        sectionIsMultiDimensional = section.hasMultiDimensionalDataElement();
        
        categoryCombo = section.getCategoryCombo();

        if ( sectionIsMultiDimensional )
        {
            optionCombos = categoryCombo.getSortedOptionCombos();

            for ( DataElementCategory dec : categoryCombo.getCategories() )
            {
                optionsMap.put( dec.getId(), dec.getCategoryOptions() );
            }

            // -----------------------------------------------------------------
            // Calculating the number of times each category should be repeated
            // -----------------------------------------------------------------

            int catColSpan = optionCombos.size();

            for ( DataElementCategory cat : categoryCombo.getCategories() )
            {
                int categoryOptionSize = cat.getCategoryOptions().size();
                
                if ( categoryOptionSize > 0 && catColSpan >= categoryOptionSize )
                {
                    catColSpan = catColSpan / categoryOptionSize;

                    int total = optionCombos.size() / ( catColSpan * categoryOptionSize );

                    Collection<Integer> cols = new ArrayList<>( total );

                    for ( int i = 0; i < total; i++ )
                    {
                        cols.add( i );
                    }

                    colRepeat.put( cat.getId(), cols );
                }
            }
        }
        else
        {
            defaultOptionComboId = categoryService.getDefaultDataElementCategoryOptionCombo().getId();
        }       

        for ( DataElementOperand operand : section.getGreyedFields() )
        {            
            greyedFields.put( operand.getDataElement().getId() + ":"  + operand.getCategoryOptionCombo().getId(), true );
        }       

        return SUCCESS;
    }
}
