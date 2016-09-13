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

import java.util.Collection;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public interface OptionService
{
    String ID = OptionService.class.getName();
   
    // -------------------------------------------------------------------------
    // OptionSet
    // -------------------------------------------------------------------------
  
    int saveOptionSet( OptionSet optionSet );

    void updateOptionSet( OptionSet optionSet );

    OptionSet getOptionSet( int id );

    OptionSet getOptionSet( String uid );

    OptionSet getOptionSetByName( String name );

    OptionSet getOptionSetByCode( String code );

    void deleteOptionSet( OptionSet optionSet );

    List<OptionSet> getAllOptionSets();

    List<Option> getOptions( String optionSetUid, String key, Integer max );
    
    List<Option> getOptions( int optionSetId, String name, Integer max );
    
    // -------------------------------------------------------------------------
    // Option
    // -------------------------------------------------------------------------

    void updateOption( Option option );
    
    Option getOption( int id );
    
    Option getOptionByCode( String code );
        
    void deleteOption( Option option  );
        
    List<Option> getOptions( OptionSet optionSet, String option, Integer min, Integer max );

    // -------------------------------------------------------------------------
    // OptionGroup
    // -------------------------------------------------------------------------

    int saveOptionGroup( OptionGroup group );

    void updateOptionGroup( OptionGroup group );

    OptionGroup getOptionGroup( int id );

    OptionGroup getOptionGroup( String uid );

    List<OptionGroup> getOptionGroupsByUid( Collection<String> uids );

    void deleteOptionGroup( OptionGroup group );

    List<OptionGroup> getAllOptionGroups();

    List<OptionGroup> getOptionGroups( OptionGroupSet groupSet );

    OptionGroup getOptionGroupByName( String name );

    OptionGroup getOptionGroupByCode( String code );

    OptionGroup getOptionGroupByShortName( String shortName );

    int getOptionGroupCount();

    int getOptionGroupCountByName( String name );

    // -------------------------------------------------------------------------
    // OptionGroupSet
    // -------------------------------------------------------------------------

    int saveOptionGroupSet( OptionGroupSet group );

    void updateOptionGroupSet( OptionGroupSet group );

    OptionGroupSet getOptionGroupSet( int id );

    OptionGroupSet getOptionGroupSet( String uid );

    List<OptionGroupSet> getOptionGroupSetsByUid( Collection<String> uids );

    void deleteOptionGroupSet( OptionGroupSet group );

    List<OptionGroupSet> getAllOptionGroupSets();

    OptionGroupSet getOptionGroupSetByName( String name );

    int getOptionGroupSetCount();

    int getOptionGroupSetCountByName( String name );

}
