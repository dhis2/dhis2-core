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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.GenericDimensionalObjectStore;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.PeriodType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kristian Nordal
 */
@Service
@AllArgsConstructor
public class DefaultDataElementService
    implements DataElementService
{
    private final DataElementStore dataElementStore;

    private final IdentifiableObjectStore<OptionSet> optionSetStore;

    private final IdentifiableObjectStore<DataElementGroup> dataElementGroupStore;

    private final GenericDimensionalObjectStore<DataElementGroupSet> dataElementGroupSetStore;

    // -------------------------------------------------------------------------
    // DataElement
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addDataElement( DataElement dataElement )
    {
        validateDateElement( dataElement );
        dataElementStore.save( dataElement );
        return dataElement.getId();
    }

    @Override
    @Transactional
    public void updateDataElement( DataElement dataElement )
    {
        validateDateElement( dataElement );
        dataElementStore.update( dataElement );
    }

    @Override
    public void validateDateElement( DataElement dataElement )
    {
        if ( dataElement.getOptionSet() == null )
        {
            if ( dataElement.getValueType() == ValueType.MULTI_TEXT )
            {
                throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1116, dataElement.getUid() ) );
            }
            return;
        }
        // need to reload the options in case this is a create and the options
        // is a shallow reference object
        OptionSet options = optionSetStore.getByUid( dataElement.getOptionSet().getUid() );
        if ( options.getValueType() != dataElement.getValueType() )
        {
            throw new IllegalQueryException( new ErrorMessage( ErrorCode.E1115, options.getValueType() ) );
        }
    }

    @Override
    @Transactional
    public void deleteDataElement( DataElement dataElement )
    {
        dataElementStore.delete( dataElement );
    }

    @Override
    @Transactional( readOnly = true )
    public DataElement getDataElement( long id )
    {
        return dataElementStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public DataElement getDataElement( String uid )
    {
        return dataElementStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public DataElement getDataElementByCode( String code )
    {
        return dataElementStore.getByCode( code );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getAllDataElements()
    {
        return dataElementStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getAllDataElementsByValueType( ValueType valueType )
    {
        return dataElementStore.getDataElementsByValueType( valueType );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getDataElementsByZeroIsSignificant( boolean zeroIsSignificant )
    {
        return dataElementStore.getDataElementsByZeroIsSignificant( zeroIsSignificant );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getDataElementsByPeriodType( final PeriodType periodType )
    {
        return getAllDataElements().stream()
            .filter( p -> p.getPeriodType() != null && p.getPeriodType().equals( periodType ) )
            .collect( Collectors.toList() );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getDataElementsByDomainType( DataElementDomain domainType )
    {
        return dataElementStore.getDataElementsByDomainType( domainType );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getDataElementByCategoryCombo( CategoryCombo categoryCombo )
    {
        return dataElementStore.getDataElementByCategoryCombo( categoryCombo );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getDataElementsWithoutGroups()
    {
        return dataElementStore.getDataElementsWithoutGroups();
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getDataElementsWithoutDataSets()
    {
        return dataElementStore.getDataElementsWithoutDataSets();
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getDataElementsWithDataSets()
    {
        return dataElementStore.getDataElementsWithDataSets();
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getDataElementsByAggregationLevel( int aggregationLevel )
    {
        return dataElementStore.getDataElementsByAggregationLevel( aggregationLevel );
    }

    // -------------------------------------------------------------------------
    // DataElementGroup
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addDataElementGroup( DataElementGroup dataElementGroup )
    {
        dataElementGroupStore.save( dataElementGroup );

        return dataElementGroup.getId();
    }

    @Override
    @Transactional
    public void updateDataElementGroup( DataElementGroup dataElementGroup )
    {
        dataElementGroupStore.update( dataElementGroup );
    }

    @Override
    @Transactional
    public void deleteDataElementGroup( DataElementGroup dataElementGroup )
    {
        dataElementGroupStore.delete( dataElementGroup );
    }

    @Override
    @Transactional( readOnly = true )
    public DataElementGroup getDataElementGroup( long id )
    {
        return dataElementGroupStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElementGroup> getDataElementGroupsByUid( Collection<String> uids )
    {
        return dataElementGroupStore.getByUid( uids );
    }

    @Override
    @Transactional( readOnly = true )
    public DataElementGroup getDataElementGroup( String uid )
    {
        return dataElementGroupStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElementGroup> getAllDataElementGroups()
    {
        return dataElementGroupStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public DataElementGroup getDataElementGroupByName( String name )
    {
        List<DataElementGroup> dataElementGroups = dataElementGroupStore.getAllEqName( name );

        return !dataElementGroups.isEmpty() ? dataElementGroups.get( 0 ) : null;
    }

    // -------------------------------------------------------------------------
    // DataElementGroupSet
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addDataElementGroupSet( DataElementGroupSet groupSet )
    {
        dataElementGroupSetStore.save( groupSet );

        return groupSet.getId();
    }

    @Override
    @Transactional
    public void updateDataElementGroupSet( DataElementGroupSet groupSet )
    {
        dataElementGroupSetStore.update( groupSet );
    }

    @Override
    @Transactional
    public void deleteDataElementGroupSet( DataElementGroupSet groupSet )
    {
        dataElementGroupSetStore.delete( groupSet );
    }

    @Override
    @Transactional( readOnly = true )
    public DataElementGroupSet getDataElementGroupSet( long id )
    {
        return dataElementGroupSetStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public DataElementGroupSet getDataElementGroupSet( String uid )
    {
        return dataElementGroupSetStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public DataElementGroupSet getDataElementGroupSetByName( String name )
    {
        List<DataElementGroupSet> dataElementGroupSets = dataElementGroupSetStore.getAllEqName( name );

        return !dataElementGroupSets.isEmpty() ? dataElementGroupSets.get( 0 ) : null;
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElementGroupSet> getAllDataElementGroupSets()
    {
        return dataElementGroupSetStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getByAttributeAndValue( Attribute attribute, String value )
    {
        return dataElementStore.getByAttributeAndValue( attribute, value );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DataElement> getByAttribute( Attribute attribute )
    {
        return dataElementStore.getByAttribute( attribute );
    }

    @Override
    @Transactional( readOnly = true )
    public DataElement getByUniqueAttributeValue( Attribute attribute, String value )
    {
        return dataElementStore.getByUniqueAttributeValue( attribute, value );
    }
}
