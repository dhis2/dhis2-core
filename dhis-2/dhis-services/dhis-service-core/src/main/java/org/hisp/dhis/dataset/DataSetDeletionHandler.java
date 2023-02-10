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
package org.hisp.dhis.dataset;

import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component
@RequiredArgsConstructor
public class DataSetDeletionHandler extends IdObjectDeletionHandler<DataSet>
{
    private final DataSetService dataSetService;

    private final CategoryService categoryService;

    @Override
    protected void registerHandler()
    {
        whenDeleting( DataElement.class, this::deleteDataElement );
        whenDeleting( Indicator.class, this::deleteIndicator );
        whenDeleting( Section.class, this::deleteSection );
        whenDeleting( LegendSet.class, this::deleteLegendSet );
        whenDeleting( CategoryCombo.class, this::deleteCategoryCombo );
        whenDeleting( OrganisationUnit.class, this::deleteOrganisationUnit );
        whenDeleting( DataEntryForm.class, this::deleteDataEntryForm );
        whenDeleting( DataApprovalWorkflow.class, this::deleteDataApprovalWorkflow );
    }

    private void deleteDataElement( DataElement dataElement )
    {
        Iterator<DataSetElement> elements = dataElement.getDataSetElements().iterator();

        while ( elements.hasNext() )
        {
            DataSetElement element = elements.next();
            elements.remove();

            dataElement.removeDataSetElement( element );
            idObjectManager.updateNoAcl( element.getDataSet() );
        }

        List<DataSet> dataSets = idObjectManager.getAllNoAcl( DataSet.class );

        for ( DataSet dataSet : dataSets )
        {
            if ( dataSet.getCompulsoryDataElementOperands().removeIf(
                operand -> operand.getDataElement().equals( dataElement ) ) )
            {
                idObjectManager.updateNoAcl( dataSet );
            }
        }
    }

    private void deleteIndicator( Indicator indicator )
    {
        for ( DataSet dataSet : indicator.getDataSets() )
        {
            dataSet.getIndicators().remove( indicator );
            idObjectManager.updateNoAcl( dataSet );
        }
    }

    private void deleteSection( Section section )
    {
        DataSet dataSet = section.getDataSet();

        if ( dataSet != null )
        {
            dataSet.getSections().remove( section );
            idObjectManager.updateNoAcl( dataSet );
        }
    }

    private void deleteLegendSet( LegendSet legendSet )
    {
        for ( DataSet dataSet : idObjectManager.getAllNoAcl( DataSet.class ) )
        {
            for ( LegendSet ls : dataSet.getLegendSets() )
            {
                if ( legendSet.equals( ls ) )
                {
                    dataSet.getLegendSets().remove( ls );
                    idObjectManager.updateNoAcl( dataSet );
                }

            }
        }
    }

    private void deleteCategoryCombo( CategoryCombo categoryCombo )
    {
        CategoryCombo defaultCategoryCombo = categoryService
            .getCategoryComboByName( DEFAULT_CATEGORY_COMBO_NAME );

        Collection<DataSet> dataSets = idObjectManager.getAllNoAcl( DataSet.class );

        for ( DataSet dataSet : dataSets )
        {
            if ( dataSet != null && categoryCombo.equals( dataSet.getCategoryCombo() ) )
            {
                dataSet.setCategoryCombo( defaultCategoryCombo );
                idObjectManager.updateNoAcl( dataSet );
            }
        }
    }

    private void deleteOrganisationUnit( OrganisationUnit unit )
    {
        for ( DataSet dataSet : unit.getDataSets() )
        {
            dataSet.getSources().remove( unit );
            idObjectManager.updateNoAcl( dataSet );
        }
    }

    private void deleteDataEntryForm( DataEntryForm dataEntryForm )
    {
        List<DataSet> associatedDataSets = dataSetService.getDataSetsByDataEntryForm( dataEntryForm );

        for ( DataSet dataSet : associatedDataSets )
        {
            dataSet.setDataEntryForm( null );
            idObjectManager.updateNoAcl( dataSet );
        }
    }

    private void deleteDataApprovalWorkflow( DataApprovalWorkflow workflow )
    {
        for ( DataSet dataSet : workflow.getDataSets() )
        {
            dataSet.setWorkflow( null );
            idObjectManager.updateNoAcl( dataSet );
        }
    }
}
