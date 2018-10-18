package org.hisp.dhis.category;

/*
 *
 *  Copyright (c) 2004-2018, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Transactional
public class DefaultCategoryManager
    implements CategoryManager
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private static final Log log = LogFactory.getLog( DefaultCategoryManager.class );

    @Autowired
    private CategoryService categoryService;

    // -------------------------------------------------------------------------
    // CategoryOptionCombo
    // -------------------------------------------------------------------------

    @Override
    public void addAndPruneOptionCombos( CategoryCombo categoryCombo )
    {
        if ( categoryCombo == null || !categoryCombo.isValid() )
        {
            log.warn( "Category combo is null or invalid, could not update option combos: " + categoryCombo );
            return;
        }

        List<CategoryOptionCombo> generatedOptionCombos = categoryCombo.generateOptionCombosList();
        Set<CategoryOptionCombo> persistedOptionCombos = Sets.newHashSet( categoryCombo.getOptionCombos() );

        boolean modified = false;

        for ( CategoryOptionCombo optionCombo : generatedOptionCombos )
        {
            if ( !persistedOptionCombos.contains( optionCombo ) )
            {
                categoryCombo.getOptionCombos().add( optionCombo );
                categoryService.addCategoryOptionCombo( optionCombo );

                log.info( "Added missing category option combo: " + optionCombo + " for category combo: " + categoryCombo.getName() );
                modified = true;
            }
        }

        Iterator<CategoryOptionCombo> iterator = persistedOptionCombos.iterator();

        while ( iterator.hasNext() )
        {
            CategoryOptionCombo optionCombo = iterator.next();

            if ( !generatedOptionCombos.contains( optionCombo ) )
            {
                try
                {
                    categoryService.deleteCategoryOptionCombo( optionCombo );
                }
                catch ( Exception ex )
                {
                    log.warn( "Could not delete category option combo: " + optionCombo );
                    continue;
                }

                iterator.remove();
                categoryCombo.getOptionCombos().remove( optionCombo );
                categoryService.deleteCategoryOptionCombo( optionCombo );

                log.info( "Deleted obsolete category option combo: " + optionCombo + " for category combo: " + categoryCombo.getName() );
                modified = true;
            }
        }

        if ( modified )
        {
            categoryService.updateCategoryCombo( categoryCombo );
        }
    }

    @Override
    public void addAndPruneAllOptionCombos()
    {
        List<CategoryCombo> categoryCombos = categoryService.getAllCategoryCombos();

        for ( CategoryCombo categoryCombo : categoryCombos )
        {
            addAndPruneOptionCombos( categoryCombo );
        }
    }
}
