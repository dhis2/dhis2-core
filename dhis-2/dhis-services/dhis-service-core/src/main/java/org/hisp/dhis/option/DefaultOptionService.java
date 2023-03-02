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
package org.hisp.dhis.option;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.option.OptionService" )
public class DefaultOptionService
    implements OptionService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private IdentifiableObjectStore<OptionSet> optionSetStore;

    private OptionStore optionStore;

    private OptionGroupStore optionGroupStore;

    private OptionGroupSetStore optionGroupSetStore;

    public DefaultOptionService(
        @Qualifier( "org.hisp.dhis.option.OptionSetStore" ) IdentifiableObjectStore<OptionSet> optionSetStore,
        OptionStore optionStore,
        OptionGroupStore optionGroupStore, OptionGroupSetStore optionGroupSetStore )
    {
        checkNotNull( optionSetStore );
        checkNotNull( optionStore );
        checkNotNull( optionGroupStore );
        checkNotNull( optionGroupSetStore );

        this.optionSetStore = optionSetStore;
        this.optionStore = optionStore;
        this.optionGroupStore = optionGroupStore;
        this.optionGroupSetStore = optionGroupSetStore;
    }

    // -------------------------------------------------------------------------
    // OptionService implementation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Option Set
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long saveOptionSet( OptionSet optionSet )
    {
        validateOptionSet( optionSet );
        optionSetStore.save( optionSet );
        return optionSet.getId();
    }

    @Override
    @Transactional
    public void updateOptionSet( OptionSet optionSet )
    {
        validateOptionSet( optionSet );
        optionSetStore.update( optionSet );
    }

    @Override
    @Transactional( readOnly = true )
    public void validateOptionSet( OptionSet optionSet )
        throws IllegalQueryException
    {
        if ( optionSet.getValueType() != ValueType.MULTI_TEXT )
        {
            return;
        }
        for ( Option option : optionSet.getOptions() )
        {
            if ( option.getCode() == null )
            {
                String uid = option.getUid();
                if ( uid != null )
                {
                    option = optionStore.getByUid( uid );
                    if ( option == null )
                    {
                        throw new IllegalQueryException( ErrorCode.E1113, Option.class.getSimpleName(), uid );
                    }
                }
            }
            if ( option.getCode() == null )
            {
                throw new IllegalQueryException( ErrorCode.E4000, "code" );
            }
            ErrorMessage error = validateOption( optionSet, option );
            if ( error != null )
            {
                throw new IllegalQueryException( error );
            }
        }
    }

    @Override
    public ErrorMessage validateOption( OptionSet optionSet, Option option )
    {
        if ( optionSet != null
            && optionSet.getValueType() == ValueType.MULTI_TEXT
            && option.getCode().contains( ValueType.MULTI_TEXT_SEPARATOR ) )
        {
            return new ErrorMessage( ErrorCode.E1118, optionSet.getUid(), option.getCode() );
        }
        return null;
    }

    @Override
    @Transactional( readOnly = true )
    public OptionSet getOptionSet( long id )
    {
        return optionSetStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public OptionSet getOptionSet( String uid )
    {
        return optionSetStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public OptionSet getOptionSetByName( String name )
    {
        return optionSetStore.getByName( name );
    }

    @Override
    @Transactional( readOnly = true )
    public OptionSet getOptionSetByCode( String code )
    {
        return optionSetStore.getByCode( code );
    }

    @Override
    @Transactional
    public void deleteOptionSet( OptionSet optionSet )
    {
        optionSetStore.delete( optionSet );
    }

    @Override
    @Transactional( readOnly = true )
    public List<OptionSet> getAllOptionSets()
    {
        return optionSetStore.getAll();
    }

    // -------------------------------------------------------------------------
    // Option
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public List<Option> getOptions( long optionSetId, String key, Integer max )
    {
        List<Option> options;

        if ( key != null || max != null )
        {
            // Use query as option set size might be very high

            options = optionStore.getOptions( optionSetId, key, max );
        }
        else
        {
            // Return all from object association to preserve custom order

            OptionSet optionSet = getOptionSet( optionSetId );

            options = new ArrayList<>( optionSet.getOptions() );
        }

        return options;
    }

    @Override
    @Transactional
    public void updateOption( Option option )
    {
        optionStore.update( option );
    }

    @Override
    @Transactional( readOnly = true )
    public Option getOption( long id )
    {
        return optionStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public Option getOptionByCode( String code )
    {
        return optionStore.getByCode( code );
    }

    @Override
    @Transactional
    public void deleteOption( Option option )
    {
        optionStore.delete( option );
    }

    // -------------------------------------------------------------------------
    // OptionGroup
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long saveOptionGroup( OptionGroup group )
    {
        optionGroupStore.save( group );

        return group.getId();
    }

    @Override
    @Transactional
    public void updateOptionGroup( OptionGroup group )
    {
        optionGroupStore.update( group );
    }

    @Override
    @Transactional( readOnly = true )
    public OptionGroup getOptionGroup( long id )
    {
        return optionGroupStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public OptionGroup getOptionGroup( String uid )
    {
        return optionGroupStore.getByUid( uid );
    }

    @Override
    @Transactional
    public void deleteOptionGroup( OptionGroup group )
    {
        optionGroupStore.delete( group );
    }

    @Override
    @Transactional( readOnly = true )
    public List<OptionGroup> getAllOptionGroups()
    {
        return optionGroupStore.getAll();
    }

    // -------------------------------------------------------------------------
    // OptionGroupSet
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long saveOptionGroupSet( OptionGroupSet group )
    {
        optionGroupSetStore.save( group );

        return group.getId();
    }

    @Override
    @Transactional
    public void updateOptionGroupSet( OptionGroupSet group )
    {
        optionGroupSetStore.update( group );
    }

    @Override
    @Transactional( readOnly = true )
    public OptionGroupSet getOptionGroupSet( long id )
    {
        return optionGroupSetStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public OptionGroupSet getOptionGroupSet( String uid )
    {
        return optionGroupSetStore.getByUid( uid );
    }

    @Override
    @Transactional
    public void deleteOptionGroupSet( OptionGroupSet group )
    {
        optionGroupSetStore.delete( group );
    }

    @Override
    @Transactional( readOnly = true )
    public List<OptionGroupSet> getAllOptionGroupSets()
    {
        return optionGroupSetStore.getAll();
    }
}
