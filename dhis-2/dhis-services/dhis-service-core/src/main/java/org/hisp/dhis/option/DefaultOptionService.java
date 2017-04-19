package org.hisp.dhis.option;

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

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultOptionService
    implements OptionService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericIdentifiableObjectStore<OptionSet> optionSetStore;

    public void setOptionSetStore( GenericIdentifiableObjectStore<OptionSet> optionSetStore )
    {
        this.optionSetStore = optionSetStore;
    }

    private OptionStore optionStore;

    public void setOptionStore( OptionStore optionStore )
    {
        this.optionStore = optionStore;
    }

    @Autowired
    private OptionGroupStore optionGroupStore;

    @Autowired
    private OptionGroupSetStore optionGroupSetStore;

    // -------------------------------------------------------------------------
    // OptionService implementation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Option Set
    // -------------------------------------------------------------------------

    @Override
    public int saveOptionSet( OptionSet optionSet )
    {
        optionSetStore.save( optionSet );

        return optionSet.getId();
    }

    @Override
    public void updateOptionSet( OptionSet optionSet )
    {
        optionSetStore.update( optionSet );
    }

    @Override
    public OptionSet getOptionSet( int id )
    {
        return optionSetStore.get( id );
    }

    @Override
    public OptionSet getOptionSet( String uid )
    {
        return optionSetStore.getByUid( uid );
    }

    @Override
    public OptionSet getOptionSetByName( String name )
    {
        return optionSetStore.getByName( name );
    }

    @Override
    public OptionSet getOptionSetByCode( String code )
    {
        return optionSetStore.getByCode( code );
    }

    @Override
    public void deleteOptionSet( OptionSet optionSet )
    {
        optionSetStore.delete( optionSet );
    }

    @Override
    public List<OptionSet> getAllOptionSets()
    {
        return optionSetStore.getAll();
    }

    // -------------------------------------------------------------------------
    // Option
    // -------------------------------------------------------------------------

    @Override
    public List<Option> getOptions( int optionSetId, String key, Integer max )
    {
        List<Option> options = null;

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
    public void updateOption( Option option )
    {
        optionStore.update( option );
    }

    @Override
    public Option getOption( int id )
    {
        return optionStore.get( id );
    }

    @Override
    public Option getOptionByCode( String code )
    {
        return optionStore.getByCode( code );
    }

    @Override
    public void deleteOption( Option option )
    {
        optionStore.delete( option );
    }

    // -------------------------------------------------------------------------
    // OptionGroup
    // -------------------------------------------------------------------------

    @Override
    public int saveOptionGroup( OptionGroup group )
    {
        optionGroupStore.save( group );

        return group.getId();
    }

    @Override
    public void updateOptionGroup( OptionGroup group )
    {
        optionGroupStore.update( group );
    }

    @Override
    public OptionGroup getOptionGroup( int id )
    {
        return optionGroupStore.get( id );
    }

    @Override
    public OptionGroup getOptionGroup( String uid )
    {
        return optionGroupStore.getByUid( uid );
    }

    @Override
    public void deleteOptionGroup( OptionGroup group )
    {
        optionGroupStore.delete( group );
    }

    @Override
    public List<OptionGroup> getAllOptionGroups()
    {
        return optionGroupStore.getAll();
    }

    @Override
    public OptionGroup getOptionGroupByName( String name )
    {
        return optionGroupStore.getByName( name );
    }

    @Override
    public OptionGroup getOptionGroupByCode( String code )
    {
        return optionGroupStore.getByCode( code );
    }

    @Override
    public OptionGroup getOptionGroupByShortName( String shortName )
    {
        List<OptionGroup> OptionGroups = new ArrayList<>(
            optionGroupStore.getAllEqShortName( shortName ) );

        if ( OptionGroups.isEmpty() )
        {
            return null;
        }

        return OptionGroups.get( 0 );
    }

    // -------------------------------------------------------------------------
    // OptionGroupSet
    // -------------------------------------------------------------------------

    @Override
    public int saveOptionGroupSet( OptionGroupSet group )
    {
        optionGroupSetStore.save( group );

        return group.getId();
    }

    @Override
    public void updateOptionGroupSet( OptionGroupSet group )
    {
        optionGroupSetStore.update( group );
    }

    @Override
    public OptionGroupSet getOptionGroupSet( int id )
    {
        return optionGroupSetStore.get( id );
    }

    @Override
    public OptionGroupSet getOptionGroupSet( String uid )
    {
        return optionGroupSetStore.getByUid( uid );
    }

    @Override
    public void deleteOptionGroupSet( OptionGroupSet group )
    {
        optionGroupSetStore.delete( group );
    }

    @Override
    public List<OptionGroupSet> getAllOptionGroupSets()
    {
        return optionGroupSetStore.getAll();
    }

    @Override
    public OptionGroupSet getOptionGroupSetByName( String name )
    {
        return optionGroupSetStore.getByName( name );
    }
}
