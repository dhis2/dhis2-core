/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dataelement;

import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;

import java.util.Iterator;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component
@RequiredArgsConstructor
public class DataElementDeletionHandler extends IdObjectDeletionHandler<DataElement>
{
    private final CategoryService categoryService;

    @Override
    protected void registerHandler()
    {
        whenDeleting( CategoryCombo.class, this::deleteCategoryCombo );
        whenDeleting( DataSet.class, this::deleteDataSet );
        whenDeleting( DataElementGroup.class, this::deleteDataElementGroup );
        whenDeleting( LegendSet.class, this::deleteLegendSet );
        whenVetoing( OptionSet.class, this::allowDeleteOptionSet );
    }

    private void deleteCategoryCombo( CategoryCombo categoryCombo )
    {
        CategoryCombo defaultCategoryCombo = categoryService
            .getCategoryComboByName( DEFAULT_CATEGORY_COMBO_NAME );

        for ( DataElement dataElement : idObjectManager.getAllNoAcl( DataElement.class ) )
        {
            if ( dataElement != null && dataElement.getCategoryCombo().equals( categoryCombo ) )
            {
                dataElement.setCategoryCombo( defaultCategoryCombo );

                idObjectManager.updateNoAcl( dataElement );
            }
        }
    }

    private void deleteDataSet( DataSet dataSet )
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

    private void deleteDataElementGroup( DataElementGroup group )
    {
        for ( DataElement element : group.getMembers() )
        {
            element.getGroups().remove( group );
            idObjectManager.updateNoAcl( element );
        }
    }

    private void deleteLegendSet( LegendSet legendSet )
    {
        for ( DataElement element : idObjectManager.getAllNoAcl( DataElement.class ) )
        {
            for ( LegendSet ls : element.getLegendSets() )
            {
                if ( legendSet.equals( ls ) )
                {
                    element.getLegendSets().remove( ls );
                    idObjectManager.updateNoAcl( element );
                }
            }
        }
    }

    private DeletionVeto allowDeleteOptionSet( OptionSet optionSet )
    {
        String sql = "select 1 from dataelement where optionsetid = :id limit 1";
        return vetoIfExists( VETO, sql, Map.of( "id", optionSet.getId() ) );
    }
}
