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

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Iterator;

import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;

/**
 * @author Lars Helge Overland
 */
public class DataElementDeletionHandler
    extends DeletionHandler
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;
    
    @Autowired
    private CategoryService categoryService;

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return DataElement.class.getSimpleName();
    }

    @Override
    public void deleteCategoryCombo( CategoryCombo categoryCombo )
    {
        CategoryCombo defaultCategoryCombo = categoryService
            .getCategoryComboByName( DEFAULT_CATEGORY_COMBO_NAME );

        for ( DataElement dataElement : idObjectManager.getAllNoAcl( DataElement.class ) )
        {
            if ( dataElement != null && dataElement.getDataElementCategoryCombo().equals( categoryCombo ) )
            {
                dataElement.setDataElementCategoryCombo( defaultCategoryCombo );

                idObjectManager.updateNoAcl( dataElement );
            }
        }
    }

    @Override
    public void deleteDataSet( DataSet dataSet )
    {
        Iterator<DataSetElement> elements = dataSet.getDataSetElements().iterator();
        
        while ( elements.hasNext() )
        {
            DataSetElement element = elements.next();
            elements.remove();
            
            dataSet.removeDataSetElement( element );
            idObjectManager.updateNoAcl( element.getDataElement() );
        }
    }

    @Override
    public void deleteDataElementGroup( DataElementGroup group )
    {
        for ( DataElement element : group.getMembers() )
        {
            element.getGroups().remove( group );
            idObjectManager.updateNoAcl( element );
        }
    }

    @Override
    public void deleteLegendSet( LegendSet legendSet )
    {
        for ( DataElement element : idObjectManager.getAllNoAcl( DataElement.class ) )
        {
            for ( LegendSet ls : element.getLegendSets() )
            {
                if( legendSet.equals( ls ) )
                {
                    element.getLegendSets().remove( ls );
                    idObjectManager.updateNoAcl( element );
                }

            }
        }
    }
    
    @Override
    public String allowDeleteOptionSet( OptionSet optionSet )
    {
        String sql = "SELECT COUNT(*) " + "FROM dataelement " + "WHERE optionsetid=" + optionSet.getId();

        return jdbcTemplate.queryForObject( sql, Integer.class ) == 0 ? null : ERROR;
    }
}
