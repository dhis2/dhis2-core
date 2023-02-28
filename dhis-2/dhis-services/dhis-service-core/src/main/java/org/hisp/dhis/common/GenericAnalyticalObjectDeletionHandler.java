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
package org.hisp.dhis.common;

import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;

/**
 * @author Lars Helge Overland
 */
public abstract class GenericAnalyticalObjectDeletionHandler<T extends BaseAnalyticalObject, S extends AnalyticalObjectService<T>>
    extends IdObjectDeletionHandler<T>
{

    protected final DeletionVeto veto;

    protected final S service;

    protected GenericAnalyticalObjectDeletionHandler( DeletionVeto veto, S service )
    {
        this.veto = veto;
        this.service = service;
    }

    protected final void deleteExpressionDimensionItem( ExpressionDimensionItem expressionDimensionItem )
    {
        removeItem( service.getAnalyticalObjects( expressionDimensionItem ), expressionDimensionItem,
            AnalyticalObject::removeDataDimensionItem );
    }

    protected final void deleteIndicator( Indicator indicator )
    {
        removeItem( service.getAnalyticalObjects( indicator ), indicator,
            AnalyticalObject::removeDataDimensionItem );
    }

    protected final void deleteDataElement( DataElement dataElement )
    {
        removeItem( service.getAnalyticalObjects( dataElement ), dataElement,
            AnalyticalObject::removeDataDimensionItem );
    }

    protected final void deleteDataSet( DataSet dataSet )
    {
        removeItem( service.getAnalyticalObjects( dataSet ), dataSet,
            AnalyticalObject::removeDataDimensionItem );
    }

    protected final void deleteProgramIndicator( ProgramIndicator programIndicator )
    {
        removeItem( service.getAnalyticalObjects( programIndicator ), programIndicator,
            AnalyticalObject::removeDataDimensionItem );
    }

    protected final void deletePeriod( Period period )
    {
        removeItem( service.getAnalyticalObjects( period ), period,
            ( ao, di ) -> ao.getPeriods().remove( di ) );
    }

    protected final DeletionVeto allowDeletePeriod( Period period )
    {
        List<T> analyticalObjects = service.getAnalyticalObjects( period );

        for ( T analyticalObject : analyticalObjects )
        {
            if ( analyticalObject.getPeriods().contains( period ) )
            {
                return VETO;
            }
        }

        return ACCEPT;
    }

    protected final void deleteOrganisationUnit( OrganisationUnit organisationUnit )
    {
        removeItem( service.getAnalyticalObjects( organisationUnit ), organisationUnit,
            ( ao, di ) -> ao.getOrganisationUnits().remove( di ) );
    }

    protected final void deleteOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup )
    {
        removeItem( service.getAnalyticalObjects( organisationUnitGroup ), organisationUnitGroup,
            ( ao, di ) -> {
                List<OrganisationUnitGroupSetDimension> dimensionsToDelete = ao.getOrganisationUnitGroupSetDimensions()
                    .stream()
                    .filter( Objects::nonNull )
                    .filter( ogsd -> ogsd.getItems().contains( organisationUnitGroup ) )
                    .collect( Collectors.toList() );
                ao.getOrganisationUnitGroupSetDimensions().removeAll( dimensionsToDelete );
            } );
    }

    protected final void deleteOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet )
    {
        removeDimensionalItem( service.getAnalyticalObjects( organisationUnitGroupSet ),
            organisationUnitGroupSet,
            ( ao, di ) -> {
                List<OrganisationUnitGroupSetDimension> dimensionsToDelete = ao.getOrganisationUnitGroupSetDimensions()
                    .stream()
                    .filter( Objects::nonNull )
                    .filter( ogsd -> ogsd.getDimension().equals( organisationUnitGroupSet ) )
                    .collect( Collectors.toList() );
                ao.getOrganisationUnitGroupSetDimensions().removeAll( dimensionsToDelete );
            } );
    }

    private void removeItem( List<T> analyticalObjects,
        DimensionalItemObject itemObject,
        BiConsumer<BaseAnalyticalObject, DimensionalItemObject> updateOperation )
    {
        for ( T analyticalObject : analyticalObjects )
        {
            updateOperation.accept( analyticalObject, itemObject );

            service.update( analyticalObject );
        }
    }

    private void removeDimensionalItem( List<T> analyticalObjects,
        DimensionalObject itemObject,
        BiConsumer<BaseAnalyticalObject, DimensionalObject> updateOperation )
    {
        for ( T analyticalObject : analyticalObjects )
        {
            updateOperation.accept( analyticalObject, itemObject );

            service.update( analyticalObject );
        }
    }
}
