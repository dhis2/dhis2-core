package org.hisp.dhis.option;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

    // -------------------------------------------------------------------------
    // OptionService implementation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Option Set
    // -------------------------------------------------------------------------

    @Override
    public int saveOptionSet( OptionSet optionSet )
    {
        return optionSetStore.save( optionSet );
    }

    @Override
    public void updateOptionSet( OptionSet optionSet )
    {
        optionSetStore.update( optionSet );
    }

    @Override
    public OptionSet getOptionSet( int id ) { return optionSetStore.get( id ); }

    @Override
    public OptionSet getOptionSet( String uid ) { return optionSetStore.getByUid( uid ); }

    @Override
    public OptionSet getOptionSetByName( String name ) { return optionSetStore.getByName( name ); }

    @Override
    public OptionSet getOptionSetByCode( String code ) { return optionSetStore.getByCode( code ); }

    @Override
    public void deleteOptionSet( OptionSet optionSet )
    {
        optionSetStore.delete( optionSet );
    }

    @Override
    public List<OptionSet> getAllOptionSets() { return optionSetStore.getAll() ; }

    // -------------------------------------------------------------------------
    // Option
    // -------------------------------------------------------------------------

    @Override
    public List<Option> getOptions( String optionSetUid, String key, Integer max )
    {
        OptionSet optionSet = getOptionSet( optionSetUid );

        return getOptions( optionSet.getId(), key, max );
    }

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
    public Option getOption( int id ) { return optionStore.get( id ); }

    @Override
    public Option getOptionByCode( String code ) { return  optionStore.getByCode( code ) ; }
    
    @Override
    public List<Option> getOptions( OptionSet optionSet, String option, Integer min, Integer max )
    {
        return optionStore.getOptions( optionSet, option, min, max ) ;
    }
    
    @Override
    public void deleteOption( Option option  )
    {
        optionStore.delete( option );
    }
    
}
