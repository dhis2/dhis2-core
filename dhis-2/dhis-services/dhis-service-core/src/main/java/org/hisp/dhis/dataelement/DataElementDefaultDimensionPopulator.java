package org.hisp.dhis.dataelement;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;

import java.util.Collection;

/**
 * When storing DataValues without associated dimensions there is a need to
 * refer to a default dimension. This populator persists a
 * CategoryCombo named by the
 * CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME property and a
 * corresponding DataElementCatoryOptionCombo which should be used for this
 * purpose.
 * 
 * @author Lars Helge Overland
 * @author Abyot Aselefew
 * @version $Id$
 */
public class DataElementDefaultDimensionPopulator
    extends TransactionContextStartupRoutine
{
    private static final Log log = LogFactory.getLog( DataElementDefaultDimensionPopulator.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private CategoryService categoryService;

    public void setCategoryService( CategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @Override
    public void executeInTransaction()
    {
        Category defaultCategory = categoryService.getCategoryByName( Category.DEFAULT_NAME );

        if ( defaultCategory == null )
        {
            categoryService.generateDefaultDimension();

            defaultCategory = categoryService.getCategoryByName( Category.DEFAULT_NAME );

            log.info( "Added default category" );
        }

        categoryService.updateCategory( defaultCategory );

        String defaultName = CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;

        CategoryCombo categoryCombo = categoryService.getCategoryComboByName( defaultName );

        if ( categoryCombo == null )
        {
            categoryService.generateDefaultDimension();

            log.info( "Added default dataelement dimension" );

            categoryCombo = categoryService.getCategoryComboByName( defaultName );
        }

        // ---------------------------------------------------------------------
        // Any data elements without dimensions need to be associated at least
        // with the default dimension
        // ---------------------------------------------------------------------

        Collection<DataElement> dataElements = dataElementService.getAllDataElements();

        for ( DataElement dataElement : dataElements )
        {
            if ( dataElement.getDataElementCategoryCombo() == null )
            {
                dataElement.setDataElementCategoryCombo( categoryCombo );

                dataElementService.updateDataElement( dataElement );
            }
        }
    }
}
