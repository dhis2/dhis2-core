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
package org.hisp.dhis.dxf2.metadata;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hibernate.Session;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.DataDimensionItem;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.preheat.PreheatService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAnalyticalObjectImportHandler implements AnalyticalObjectImportHandler
{
    private final PreheatService preheatService;

    private final IdentifiableObjectManager objectManager;

    @Override
    public void handleAnalyticalObject( Session session, Schema schema, BaseAnalyticalObject analyticalObject,
        ObjectBundle bundle )
    {
        handleDataDimensionItems( session, schema, analyticalObject, bundle );
        handleCategoryDimensions( session, schema, analyticalObject, bundle );
        handleDataElementDimensions( session, schema, analyticalObject, bundle );
        handleAttributeDimensions( session, schema, analyticalObject, bundle );
        handleProgramIndicatorDimensions( session, schema, analyticalObject, bundle );
        handleVisualizationLegendSet( schema, analyticalObject, bundle );
    }

    /**
     * When saving a {@link Visualization} if its property legendDefinitions has
     * reference to a {@link LegendSet} then we need to make sure that LegendSet
     * object existed in database.
     *
     * @param schema current analytic object {@link Schema}
     * @param analyticalObject the analytic object to be processed
     * @param bundle current {@link ObjectBundle}
     */
    private void handleVisualizationLegendSet( Schema schema, BaseAnalyticalObject analyticalObject,
        ObjectBundle bundle )
    {
        if ( !schema.getKlass().isAssignableFrom( Visualization.class ) )
        {
            return;
        }

        Visualization visualization = (Visualization) analyticalObject;

        if ( visualization.getLegendDefinitions() == null
            || visualization.getLegendDefinitions().getLegendSet() == null )
        {
            return;
        }

        String legendSetId = visualization.getLegendDefinitions().getLegendSet().getUid();
        LegendSet legendSet = bundle.getPreheat().get( bundle.getPreheatIdentifier(), LegendSet.class, legendSetId );
        legendSet = legendSet != null ? legendSet : objectManager.get( LegendSet.class, legendSetId );

        if ( legendSet == null )
        {
            preheatService.connectReferences( visualization.getLegendDefinitions().getLegendSet(), bundle.getPreheat(),
                bundle.getPreheatIdentifier() );
            objectManager.save( visualization.getLegendDefinitions().getLegendSet() );
        }
        else
        {
            visualization.getLegendDefinitions().setLegendSet( legendSet );
        }

        bundle.getPreheat().put( bundle.getPreheatIdentifier(), visualization.getLegendDefinitions().getLegendSet() );

    }

    private void handleDataDimensionItems( Session session, Schema schema, BaseAnalyticalObject analyticalObject,
        ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "dataDimensionItems" ) )
            return;

        for ( DataDimensionItem dataDimensionItem : analyticalObject.getDataDimensionItems() )
        {
            if ( dataDimensionItem == null )
            {
                continue;
            }

            dataDimensionItem.setDataElement(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataDimensionItem.getDataElement() ) );
            dataDimensionItem.setIndicator(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataDimensionItem.getIndicator() ) );
            dataDimensionItem.setProgramIndicator(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataDimensionItem.getProgramIndicator() ) );

            if ( dataDimensionItem.getDataElementOperand() != null )
            {
                preheatService.connectReferences( dataDimensionItem.getDataElementOperand(), bundle.getPreheat(),
                    bundle.getPreheatIdentifier() );
            }

            if ( dataDimensionItem.getReportingRate() != null )
            {
                dataDimensionItem.getReportingRate().setDataSet( bundle.getPreheat().get( bundle.getPreheatIdentifier(),
                    dataDimensionItem.getReportingRate().getDataSet() ) );
            }

            if ( dataDimensionItem.getProgramDataElement() != null )
            {
                dataDimensionItem.getProgramDataElement()
                    .setProgram( bundle.getPreheat().get( bundle.getPreheatIdentifier(),
                        dataDimensionItem.getProgramDataElement().getProgram() ) );
                dataDimensionItem.getProgramDataElement()
                    .setDataElement( bundle.getPreheat().get( bundle.getPreheatIdentifier(),
                        dataDimensionItem.getProgramDataElement().getDataElement() ) );
            }

            if ( dataDimensionItem.getProgramAttribute() != null )
            {
                dataDimensionItem.getProgramAttribute()
                    .setProgram( bundle.getPreheat().get( bundle.getPreheatIdentifier(),
                        dataDimensionItem.getProgramAttribute().getProgram() ) );
                dataDimensionItem.getProgramAttribute()
                    .setAttribute( bundle.getPreheat().get( bundle.getPreheatIdentifier(),
                        dataDimensionItem.getProgramAttribute().getAttribute() ) );
            }

            preheatService.connectReferences( dataDimensionItem, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( dataDimensionItem );
        }
    }

    private void handleCategoryDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject,
        ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "categoryDimensions" ) )
            return;

        for ( CategoryDimension categoryDimension : analyticalObject.getCategoryDimensions() )
        {
            if ( categoryDimension == null )
            {
                continue;
            }

            categoryDimension.setDimension(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), categoryDimension.getDimension() ) );
            List<CategoryOption> categoryOptions = new ArrayList<>( categoryDimension.getItems() );
            categoryDimension.getItems().clear();

            categoryOptions.forEach( co -> {
                CategoryOption categoryOption = bundle.getPreheat().get( bundle.getPreheatIdentifier(), co );
                if ( categoryOption != null )
                    categoryDimension.getItems().add( categoryOption );
            } );

            preheatService.connectReferences( categoryDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );
            session.save( categoryDimension );
        }
    }

    private void handleDataElementDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject,
        ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "dataElementDimensions" ) )
            return;

        for ( TrackedEntityDataElementDimension dataElementDimension : analyticalObject.getDataElementDimensions() )
        {
            if ( dataElementDimension == null )
            {
                continue;
            }

            dataElementDimension.setDataElement(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataElementDimension.getDataElement() ) );
            dataElementDimension.setLegendSet(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataElementDimension.getLegendSet() ) );
            dataElementDimension.setProgramStage(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), dataElementDimension.getProgramStage() ) );

            preheatService.connectReferences( dataElementDimension, bundle.getPreheat(),
                bundle.getPreheatIdentifier() );
            session.save( dataElementDimension );
        }
    }

    private void handleAttributeDimensions( Session session, Schema schema, BaseAnalyticalObject analyticalObject,
        ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "attributeDimensions" ) )
            return;

        for ( TrackedEntityAttributeDimension attributeDimension : analyticalObject.getAttributeDimensions() )
        {
            if ( attributeDimension == null )
            {
                continue;
            }

            attributeDimension.setAttribute(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), attributeDimension.getAttribute() ) );
            attributeDimension.setLegendSet(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), attributeDimension.getLegendSet() ) );

            preheatService.connectReferences( attributeDimension, bundle.getPreheat(), bundle.getPreheatIdentifier() );

            session.save( attributeDimension );
        }
    }

    private void handleProgramIndicatorDimensions( Session session, Schema schema,
        BaseAnalyticalObject analyticalObject, ObjectBundle bundle )
    {
        if ( !schema.havePersistedProperty( "programIndicatorDimensions" ) )
            return;

        for ( TrackedEntityProgramIndicatorDimension programIndicatorDimension : analyticalObject
            .getProgramIndicatorDimensions() )
        {
            if ( programIndicatorDimension == null )
            {
                continue;
            }

            programIndicatorDimension.setProgramIndicator( bundle.getPreheat().get( bundle.getPreheatIdentifier(),
                programIndicatorDimension.getProgramIndicator() ) );
            programIndicatorDimension.setLegendSet(
                bundle.getPreheat().get( bundle.getPreheatIdentifier(), programIndicatorDimension.getLegendSet() ) );

            preheatService.connectReferences( programIndicatorDimension, bundle.getPreheat(),
                bundle.getPreheatIdentifier() );
            session.save( programIndicatorDimension );
        }
    }
}
