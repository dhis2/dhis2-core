package org.hisp.dhis.dataelement;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.common.GenericDimensionalObjectStore;
import org.hisp.dhis.common.GenericNameableObjectStore;
import org.hisp.dhis.period.PeriodType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kristian Nordal
 */
@Transactional
public class DefaultDataElementService
    implements DataElementService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataElementStore dataElementStore;

    public void setDataElementStore( DataElementStore dataElementStore )
    {
        this.dataElementStore = dataElementStore;
    }

    private GenericNameableObjectStore<DataElementGroup> dataElementGroupStore;

    public void setDataElementGroupStore( GenericNameableObjectStore<DataElementGroup> dataElementGroupStore )
    {
        this.dataElementGroupStore = dataElementGroupStore;
    }

    private GenericDimensionalObjectStore<DataElementGroupSet> dataElementGroupSetStore;

    public void setDataElementGroupSetStore( GenericDimensionalObjectStore<DataElementGroupSet> dataElementGroupSetStore )
    {
        this.dataElementGroupSetStore = dataElementGroupSetStore;
    }

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    @Override
    public int addDataElement( DataElement dataElement )
    {
        dataElementStore.save( dataElement );

        return dataElement.getId();
    }

    @Override
    public void updateDataElement( DataElement dataElement )
    {
        dataElementStore.update( dataElement );
    }

    @Override
    public void deleteDataElement( DataElement dataElement )
    {
        dataElementStore.delete( dataElement );
    }

    @Override
    public DataElement getDataElement( int id )
    {
        return dataElementStore.get( id );
    }

    @Override
    public DataElement getDataElement( String uid )
    {
        return dataElementStore.getByUid( uid );
    }

    @Override
    public DataElement getDataElementByCode( String code )
    {
        return dataElementStore.getByCode( code );
    }

    @Override
    public List<DataElement> getAllDataElements()
    {
        return dataElementStore.getAll();
    }

    @Override
    public List<DataElement> getDataElementsByZeroIsSignificant( boolean zeroIsSignificant )
    {
        return dataElementStore.getDataElementsByZeroIsSignificant( zeroIsSignificant );
    }

    @Override
    public List<DataElement> getDataElementsByPeriodType( final PeriodType periodType )
    {
        return getAllDataElements().stream().filter( p -> p.getPeriodType() != null && p.getPeriodType().equals( periodType ) ).collect( Collectors.toList() );
    }

    @Override
    public List<DataElement> getDataElementsByDomainType( DataElementDomain domainType )
    {
        return dataElementStore.getDataElementsByDomainType( domainType );
    }

    @Override
    public List<DataElement> getDataElementByCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        return dataElementStore.getDataElementByCategoryCombo( categoryCombo );
    }

    @Override
    public List<DataElement> getDataElementsWithoutGroups()
    {
        return dataElementStore.getDataElementsWithoutGroups();
    }

    @Override
    public List<DataElement> getDataElementsWithoutDataSets()
    {
        return dataElementStore.getDataElementsWithoutDataSets();
    }

    @Override
    public List<DataElement> getDataElementsWithDataSets()
    {
        return dataElementStore.getDataElementsWithDataSets();
    }

    @Override
    public List<DataElement> getDataElementsByAggregationLevel( int aggregationLevel )
    {
        return dataElementStore.getDataElementsByAggregationLevel( aggregationLevel );
    }

    // -------------------------------------------------------------------------
    // DataElementGroup
    // -------------------------------------------------------------------------

    @Override
    public int addDataElementGroup( DataElementGroup dataElementGroup )
    {
        dataElementGroupStore.save( dataElementGroup );

        return dataElementGroup.getId();
    }

    @Override
    public void updateDataElementGroup( DataElementGroup dataElementGroup )
    {
        dataElementGroupStore.update( dataElementGroup );
    }

    @Override
    public void deleteDataElementGroup( DataElementGroup dataElementGroup )
    {
        dataElementGroupStore.delete( dataElementGroup );
    }

    @Override
    public DataElementGroup getDataElementGroup( int id )
    {
        return dataElementGroupStore.get( id );
    }

    @Override
    public List<DataElementGroup> getDataElementGroupsByUid( Collection<String> uids )
    {
        return dataElementGroupStore.getByUid( uids );
    }

    @Override
    public DataElementGroup getDataElementGroup( String uid )
    {
        return dataElementGroupStore.getByUid( uid );
    }

    @Override
    public List<DataElementGroup> getAllDataElementGroups()
    {
        return dataElementGroupStore.getAll();
    }

    @Override
    public DataElementGroup getDataElementGroupByName( String name )
    {
        List<DataElementGroup> dataElementGroups = dataElementGroupStore.getAllEqName( name );

        return !dataElementGroups.isEmpty() ? dataElementGroups.get( 0 ) : null;
    }

    @Override
    public DataElementGroup getDataElementGroupByShortName( String shortName )
    {
        List<DataElementGroup> dataElementGroups = dataElementGroupStore.getAllEqShortName( shortName );

        if ( dataElementGroups.isEmpty() )
        {
            return null;
        }

        return dataElementGroups.get( 0 );
    }

    @Override
    public DataElementGroup getDataElementGroupByCode( String code )
    {
        return dataElementGroupStore.getByCode( code );
    }

    // -------------------------------------------------------------------------
    // DataElementGroupSet
    // -------------------------------------------------------------------------

    @Override
    public int addDataElementGroupSet( DataElementGroupSet groupSet )
    {
        dataElementGroupSetStore.save( groupSet );

        return groupSet.getId();
    }

    @Override
    public void updateDataElementGroupSet( DataElementGroupSet groupSet )
    {
        dataElementGroupSetStore.update( groupSet );
    }

    @Override
    public void deleteDataElementGroupSet( DataElementGroupSet groupSet )
    {
        dataElementGroupSetStore.delete( groupSet );
    }

    @Override
    public DataElementGroupSet getDataElementGroupSet( int id )
    {
        return dataElementGroupSetStore.get( id );
    }

    @Override
    public DataElementGroupSet getDataElementGroupSet( String uid )
    {
        return dataElementGroupSetStore.getByUid( uid );
    }

    @Override
    public DataElementGroupSet getDataElementGroupSetByName( String name )
    {
        List<DataElementGroupSet> dataElementGroupSets = dataElementGroupSetStore.getAllEqName( name );

        return !dataElementGroupSets.isEmpty() ? dataElementGroupSets.get( 0 ) : null;
    }

    @Override
    public List<DataElementGroupSet> getAllDataElementGroupSets()
    {
        return dataElementGroupSetStore.getAll();
    }
}
