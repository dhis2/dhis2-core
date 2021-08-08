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

import java.util.Iterator;

import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component( "org.hisp.dhis.category.CategoryOptionComboDeletionHandler" )
public class CategoryOptionComboDeletionHandler
    extends DeletionHandler
{
    private static final DeletionVeto VETO = new DeletionVeto( CategoryOptionCombo.class );

    private final CategoryService categoryService;

    private final JdbcTemplate jdbcTemplate;

    public CategoryOptionComboDeletionHandler( CategoryService categoryService, JdbcTemplate jdbcTemplate )
    {
        checkNotNull( categoryService );
        checkNotNull( jdbcTemplate );

        this.categoryService = categoryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // TODO expressionoptioncombo

    @Override
    protected void register()
    {
        whenVetoing( CategoryOption.class, this::allowDeleteCategoryOption );
        whenVetoing( CategoryCombo.class, this::allowDeleteCategoryCombo );
        whenDeleting( CategoryOption.class, this::deleteCategoryOption );
        whenDeleting( CategoryCombo.class, this::deleteCategoryCombo );
    }

    private DeletionVeto allowDeleteCategoryOption( CategoryOption categoryOption )
    {
        final String dvSql = "select count(*) from datavalue dv " +
            "where dv.categoryoptioncomboid in ( " +
            "select cc.categoryoptioncomboid from categoryoptioncombos_categoryoptions cc " +
            "where cc.categoryoptionid = " + categoryOption.getId() + " ) " +
            "or dv.attributeoptioncomboid in ( " +
            "select cc.categoryoptioncomboid from categoryoptioncombos_categoryoptions cc " +
            "where cc.categoryoptionid = " + categoryOption.getId() + " );";

        if ( jdbcTemplate.queryForObject( dvSql, Integer.class ) > 0 )
        {
            return VETO;
        }

        final String crSql = "select count(*) from completedatasetregistration cdr " +
            "where cdr.attributeoptioncomboid in ( " +
            "select cc.categoryoptioncomboid from categoryoptioncombos_categoryoptions cc " +
            "where cc.categoryoptionid = " + categoryOption.getId() + " );";

        if ( jdbcTemplate.queryForObject( crSql, Integer.class ) > 0 )
        {
            return VETO;
        }

        return ACCEPT;
    }

    private DeletionVeto allowDeleteCategoryCombo( CategoryCombo categoryCombo )
    {
        final String dvSql = "select count(*) from datavalue dv " +
            "where dv.categoryoptioncomboid in ( " +
            "select co.categoryoptioncomboid from categorycombos_optioncombos co " +
            "where co.categorycomboid=" + categoryCombo.getId() + " ) " +
            "or dv.attributeoptioncomboid in ( " +
            "select co.categoryoptioncomboid from categorycombos_optioncombos co " +
            "where co.categorycomboid=" + categoryCombo.getId() + " );";

        if ( jdbcTemplate.queryForObject( dvSql, Integer.class ) > 0 )
        {
            return VETO;
        }

        final String crSql = "select count(*) from completedatasetregistration cdr " +
            "where cdr.attributeoptioncomboid in ( " +
            "select co.categoryoptioncomboid from categorycombos_optioncombos co " +
            "where co.categorycomboid=" + categoryCombo.getId() + " );";

        if ( jdbcTemplate.queryForObject( crSql, Integer.class ) > 0 )
        {
            return VETO;
        }

        return ACCEPT;
    }

    private void deleteCategoryOption( CategoryOption categoryOption )
    {
        Iterator<CategoryOptionCombo> iterator = categoryOption.getCategoryOptionCombos().iterator();

        while ( iterator.hasNext() )
        {
            CategoryOptionCombo optionCombo = iterator.next();
            iterator.remove();
            categoryService.deleteCategoryOptionCombo( optionCombo );
        }
    }

    private void deleteCategoryCombo( CategoryCombo categoryCombo )
    {
        Iterator<CategoryOptionCombo> iterator = categoryCombo.getOptionCombos().iterator();

        while ( iterator.hasNext() )
        {
            CategoryOptionCombo optionCombo = iterator.next();
            iterator.remove();
            categoryService.deleteCategoryOptionCombo( optionCombo );
        }
    }
}
