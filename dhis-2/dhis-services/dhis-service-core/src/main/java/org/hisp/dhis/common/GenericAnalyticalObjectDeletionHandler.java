/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.deletion.DeletionHandler;

/**
 * @author Lars Helge Overland
 */
public abstract class GenericAnalyticalObjectDeletionHandler<T extends BaseAnalyticalObject>
    extends DeletionHandler
{
    protected abstract AnalyticalObjectService<T> getAnalyticalObjectService();

    @Override
    public void deleteIndicator( Indicator indicator )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( indicator ), indicator,
            AnalyticalObject::removeDataDimensionItem );
    }

    @Override
    public void deleteDataElement( DataElement dataElement )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( dataElement ), dataElement,
            AnalyticalObject::removeDataDimensionItem );
    }

    @Override
    public void deleteDataSet( DataSet dataSet )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( dataSet ), dataSet,
            AnalyticalObject::removeDataDimensionItem );
    }

    @Override
    public void deleteProgramIndicator( ProgramIndicator programIndicator )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( programIndicator ), programIndicator,
            AnalyticalObject::removeDataDimensionItem );
    }

    @Override
    public void deletePeriod( Period period )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( period ), period,
            ( ao, di ) -> ao.getPeriods().remove( di ) );
    }

    @Override
    public String allowDeletePeriod( Period period )
    {
        List<T> analyticalObjects = getAnalyticalObjectService().getAnalyticalObjects( period );

        for ( T analyticalObject : analyticalObjects )
        {
            if ( analyticalObject.getPeriods().contains( period ) )
            {
                return ERROR;
            }
        }

        return null;
    }

    @Override
    public void deleteOrganisationUnit( OrganisationUnit organisationUnit )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( organisationUnit ), organisationUnit,
            ( ao, di ) -> ao.getOrganisationUnits().remove( di ) );
    }

    @Override
    public void deleteOrganisationUnitGroup( OrganisationUnitGroup organisationUnitGroup )
    {
        removeItem( getAnalyticalObjectService().getAnalyticalObjects( organisationUnitGroup ), organisationUnitGroup,
            ( ao, di ) -> {
                List<OrganisationUnitGroupSetDimension> dimensionsToDelete = ao.getOrganisationUnitGroupSetDimensions()
                    .stream()
                    .filter( Objects::nonNull )
                    .filter( ogsd -> ogsd.getItems().contains( organisationUnitGroup ) )
                    .collect( Collectors.toList() );
                ao.getOrganisationUnitGroupSetDimensions().removeAll( dimensionsToDelete );
            } );
    }

    @Override
    public void deleteOrganisationUnitGroupSet( OrganisationUnitGroupSet organisationUnitGroupSet )
    {
        removeDimensionalItem( getAnalyticalObjectService().getAnalyticalObjects( organisationUnitGroupSet ),
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

    protected void removeItem( List<T> analyticalObjects, DimensionalItemObject itemObject,
        BiConsumer<BaseAnalyticalObject, DimensionalItemObject> updateOperation )
    {
        for ( T analyticalObject : analyticalObjects )
        {
            updateOperation.accept( analyticalObject, itemObject );

            getAnalyticalObjectService().update( analyticalObject );
        }
    }

    protected void removeDimensionalItem( List<T> analyticalObjects, DimensionalObject itemObject,
        BiConsumer<BaseAnalyticalObject, DimensionalObject> updateOperation )
    {
        for ( T analyticalObject : analyticalObjects )
        {
            updateOperation.accept( analyticalObject, itemObject );

            getAnalyticalObjectService().update( analyticalObject );
        }
    }
}
