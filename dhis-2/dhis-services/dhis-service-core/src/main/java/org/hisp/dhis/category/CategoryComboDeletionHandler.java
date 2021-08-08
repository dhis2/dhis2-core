/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.category;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Component( "org.hisp.dhis.category.CategoryComboDeletionHandler" )
public class CategoryComboDeletionHandler
    extends
    DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    private final IdentifiableObjectManager idObjectManager;

    private final CategoryService categoryService;

    public CategoryComboDeletionHandler( CategoryService categoryService, IdentifiableObjectManager idObjectManager )
    {
        checkNotNull( categoryService );
        checkNotNull( idObjectManager );
        this.categoryService = categoryService;
        this.idObjectManager = idObjectManager;
    }

    @Override
    protected void register()
    {
        whenVetoing( Category.class, this::allowDeleteCategory );
        whenDeleting( CategoryOptionCombo.class, this::deleteCategoryOptionCombo );
    }

    private DeletionVeto allowDeleteCategory( Category category )
    {
        for ( CategoryCombo categoryCombo : categoryService.getAllCategoryCombos() )
        {
            if ( categoryCombo.getCategories().contains( category ) )
            {
                return new DeletionVeto( CategoryCombo.class, categoryCombo.getName() );
            }
        }
        return ACCEPT;
    }

    private void deleteCategoryOptionCombo( CategoryOptionCombo categoryOptionCombo )
    {
        for ( CategoryCombo categoryCombo : categoryService.getAllCategoryCombos() )
        {
            categoryCombo.getOptionCombos().remove( categoryOptionCombo );
            idObjectManager.updateNoAcl( categoryCombo );
        }
    }
}
