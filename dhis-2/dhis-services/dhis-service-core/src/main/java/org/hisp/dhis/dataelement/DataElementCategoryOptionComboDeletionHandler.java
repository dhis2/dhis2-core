package org.hisp.dhis.dataelement;

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

import java.util.Iterator;

import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DataElementCategoryOptionComboDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    //TODO expressionoptioncombo
    
    @Override
    public String getClassName()
    {
        return DataElementCategoryOptionCombo.class.getSimpleName();
    }
    
    @Override
    public String allowDeleteDataElementCategoryOption( DataElementCategoryOption categoryOption )
    {
        final String dvSql =
            "select count(*) from datavalue dv " +
            "where dv.categoryoptioncomboid in ( " +
                "select cc.categoryoptioncomboid from categoryoptioncombos_categoryoptions cc " +
                "where cc.categoryoptionid = " + categoryOption.getId() + " ) " +
            "or dv.attributeoptioncomboid in ( " +
                "select cc.categoryoptioncomboid from categoryoptioncombos_categoryoptions cc " +
                "where cc.categoryoptionid = " + categoryOption.getId() + " );";
        
        if ( jdbcTemplate.queryForObject( dvSql, Integer.class ) > 0 )
        {
            return ERROR;
        }
        
        final String crSql = 
            "select count(*) from completedatasetregistration cdr " +
            "where cdr.attributeoptioncomboid in ( " +
                "select cc.categoryoptioncomboid from categoryoptioncombos_categoryoptions cc " +
                "where cc.categoryoptionid = " + categoryOption.getId() + " );";
        
        if ( jdbcTemplate.queryForObject( crSql, Integer.class ) > 0 )
        {
            return ERROR;
        }
        
        return null;
    }
    
    @Override
    public String allowDeleteDataElementCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        final String dvSql =
            "select count(*) from datavalue dv " +
            "where dv.categoryoptioncomboid in ( " +
                "select co.categoryoptioncomboid from categorycombos_optioncombos co " +
                "where co.categorycomboid=" + categoryCombo.getId() + " ) " +
            "or dv.attributeoptioncomboid in ( " +
                "select co.categoryoptioncomboid from categorycombos_optioncombos co " +
                "where co.categorycomboid=" + categoryCombo.getId() + " );";
        
        if ( jdbcTemplate.queryForObject( dvSql, Integer.class ) > 0 )
        {
            return ERROR;
        }
        
        final String crSql =
            "select count(*) from completedatasetregistration cdr " +
            "where cdr.attributeoptioncomboid in ( " +
                "select co.categoryoptioncomboid from categorycombos_optioncombos co " +
                "where co.categorycomboid=" + categoryCombo.getId() + " );";
        
        if ( jdbcTemplate.queryForObject( crSql, Integer.class ) > 0 )
        {
            return ERROR;
        }
        
        return null;
    }
    
    @Override
    public void deleteDataElementCategoryOption( DataElementCategoryOption categoryOption )
    {
        Iterator<DataElementCategoryOptionCombo> iterator = categoryOption.getCategoryOptionCombos().iterator();

        while ( iterator.hasNext() )
        {
            DataElementCategoryOptionCombo optionCombo = iterator.next();
            iterator.remove();
            categoryService.deleteDataElementCategoryOptionCombo( optionCombo );
        }
    }
    
    @Override
    public void deleteDataElementCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        Iterator<DataElementCategoryOptionCombo> iterator = categoryCombo.getOptionCombos().iterator();
        
        while ( iterator.hasNext() )
        {
            DataElementCategoryOptionCombo optionCombo = iterator.next();
            iterator.remove();
            categoryService.deleteDataElementCategoryOptionCombo( optionCombo );
        }
    }
}
