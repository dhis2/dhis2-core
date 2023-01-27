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
package org.hisp.dhis.expressiondimensionitem;

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.springframework.stereotype.Component;

/**
 * @author maikel arabori
 */
@AllArgsConstructor
@Component
public class ExpressionDimensionItemDeletionHandler extends DeletionHandler
{
    private final VisualizationService visualizationService;

    @Override
    protected void register()
    {
        whenDeleting( ExpressionDimensionItem.class, this::deleteFromVisualization );
    }

    private void deleteFromVisualization( ExpressionDimensionItem expressionDimensionItem )
    {
        List<Visualization> visualizations = visualizationService.getAll()
            .stream().filter( v -> v.getDataDimensionItems()
                .stream().anyMatch( it -> {
                    if ( it.getExpressionDimensionItem() == null )
                    {
                        return false;
                    }

                    return it.getExpressionDimensionItem().getUid().equals( expressionDimensionItem.getUid() );
                } ) )
            .collect( Collectors.toList() );

        for ( Visualization visualization : visualizations )
        {
            List<String> columns = visualization.getColumnDimensions();
            columns.remove( expressionDimensionItem.getUid() );

            List<String> rows = visualization.getRowDimensions();
            rows.remove( expressionDimensionItem.getUid() );

            List<String> filters = visualization.getFilterDimensions();
            filters.remove( expressionDimensionItem.getUid() );

            List<DataDimensionItem> dataDimensionItems = visualization.getDataDimensionItems();

            ListIterator<DataDimensionItem> it = dataDimensionItems.listIterator();

            while ( it.hasNext() )
            {
                DataDimensionItem dataDimensionItem = it.next();

                if ( dataDimensionItem != null && dataDimensionItem.getExpressionDimensionItem() != null )
                {
                    String dimensionUid = dataDimensionItem.getExpressionDimensionItem().getDimensionItem();

                    if ( expressionDimensionItem.getUid().equals( dimensionUid ) )
                    {
                        it.remove();
                    }
                }
            }
        }
    }
}