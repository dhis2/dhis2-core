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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test of {@link OptionObjectBundleHook}.
 *
 * @author Volker Schmidt
 */
public class OptionObjectBundleHookTest
{
    private OptionObjectBundleHook hook = new OptionObjectBundleHook();

    private Preheat preheat = new Preheat();

    @Test
    public void preCreate()
    {
        OptionSet optionSet = new OptionSet();
        optionSet.setUid( "jadhjSHdhs" );
        Option option = new Option();
        option.setOptionSet( optionSet );

        OptionSet persistedOptionSet = new OptionSet();
        persistedOptionSet.setUid( "jadhjSHdhs" );
        preheat.put( PreheatIdentifier.UID, persistedOptionSet );

        ObjectBundleParams objectBundleParams = new ObjectBundleParams();
        objectBundleParams.setPreheatIdentifier( PreheatIdentifier.UID );
        ObjectBundle bundle = new ObjectBundle( objectBundleParams, preheat, Collections.emptyMap() );
        hook.preCreate( option, bundle );

        Assert.assertEquals( 1, persistedOptionSet.getOptions().size() );
        Assert.assertSame( option, persistedOptionSet.getOptions().get( 0 ) );
    }

    @Test
    public void preCreateOptionSetAvailable()
    {
        OptionSet optionSet = new OptionSet();
        optionSet.setUid( "jadhjSHdhs" );
        Option option = new Option();
        option.setOptionSet( optionSet );

        OptionSet persistedOptionSet = new OptionSet();
        persistedOptionSet.setUid( "jadhjSHdhs" );
        preheat.put( PreheatIdentifier.UID, persistedOptionSet );

        ObjectBundleParams objectBundleParams = new ObjectBundleParams();
        objectBundleParams.setPreheatIdentifier( PreheatIdentifier.UID );
        ObjectBundle bundle = new ObjectBundle( objectBundleParams, preheat,
            Collections.singletonMap( OptionSet.class, Collections.singletonList( optionSet ) ) );
        hook.preCreate( option, bundle );

        Assert.assertEquals( 0, persistedOptionSet.getOptions().size() );
    }

    @Test
    public void validate()
    {
        OptionSet optionSet = new OptionSet();
        optionSet.setUid( "optionSet1" );

        Option option = new Option();
        option.setName( "optionName" );
        option.setCode( "optionCode" );
        optionSet.addOption( option );

        OptionSet persistedOptionSet = new OptionSet();
        persistedOptionSet.setUid( "optionSet1" );

        Option persistedOption = new Option();
        persistedOption.setName( "optionName" );
        persistedOption.setCode( "optionCode" );
        persistedOptionSet.addOption( persistedOption );

        preheat.put( PreheatIdentifier.UID, persistedOptionSet );

        ObjectBundleParams objectBundleParams = new ObjectBundleParams();
        objectBundleParams.setPreheatIdentifier( PreheatIdentifier.UID );
        ObjectBundle bundle = new ObjectBundle( objectBundleParams, preheat,
            Collections.singletonMap( OptionSet.class, Collections.singletonList( persistedOptionSet ) ) );

        List<ErrorReport> errors = hook.validate( option, bundle );

        Assert.assertNotNull( errors );
        Assert.assertEquals( 1, errors.size() );
        Assert.assertEquals( ErrorCode.E4028, errors.get( 0 ).getErrorCode() );
    }
}