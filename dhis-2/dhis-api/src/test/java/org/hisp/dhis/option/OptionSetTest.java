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
package org.hisp.dhis.option;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link OptionSet}.
 *
 * @author Volker Schmidt
 */
public class OptionSetTest
{
    @Test
    public void addOptionWithoutOrder()
    {
        final OptionSet optionSet = new OptionSet();
        final Option option1 = new Option();
        final Option option2 = new Option();

        optionSet.addOption( option1 );
        optionSet.addOption( option2 );

        Assert.assertEquals( 2, optionSet.getOptions().size() );
        Assert.assertSame( option1, optionSet.getOptions().get( 0 ) );
        Assert.assertSame( option2, optionSet.getOptions().get( 1 ) );
    }

    @Test
    public void addOptionWithLowerOrder()
    {
        final OptionSet optionSet = new OptionSet();
        final Option option1 = new Option();
        option1.setSortOrder( 10 );
        final Option option2 = new Option();
        option2.setSortOrder( 11 );
        final Option option3 = new Option();
        option3.setSortOrder( 5 );

        optionSet.addOption( option1 );
        optionSet.addOption( option2 );
        optionSet.addOption( option3 );

        Assert.assertEquals( 3, optionSet.getOptions().size() );
        Assert.assertSame( option3, optionSet.getOptions().get( 0 ) );
        Assert.assertSame( option1, optionSet.getOptions().get( 1 ) );
        Assert.assertSame( option2, optionSet.getOptions().get( 2 ) );
    }

    @Test
    public void addOptionWithHigherOrder()
    {
        final OptionSet optionSet = new OptionSet();
        final Option option1 = new Option();
        option1.setSortOrder( 10 );
        final Option option2 = new Option();
        option2.setSortOrder( 11 );
        final Option option3 = new Option();
        option3.setSortOrder( 12 );

        optionSet.addOption( option1 );
        optionSet.addOption( option2 );
        optionSet.addOption( option3 );

        Assert.assertEquals( 3, optionSet.getOptions().size() );
        Assert.assertSame( option1, optionSet.getOptions().get( 0 ) );
        Assert.assertSame( option2, optionSet.getOptions().get( 1 ) );
        Assert.assertSame( option3, optionSet.getOptions().get( 2 ) );
    }

    @Test
    public void addOptionWithSameOrder()
    {
        final OptionSet optionSet = new OptionSet();
        final Option option1 = new Option();
        option1.setSortOrder( 10 );
        final Option option2 = new Option();
        option2.setSortOrder( 11 );
        final Option option3 = new Option();
        option3.setSortOrder( 11 );

        optionSet.addOption( option1 );
        optionSet.addOption( option2 );
        optionSet.addOption( option3 );

        Assert.assertEquals( 3, optionSet.getOptions().size() );
        Assert.assertSame( option1, optionSet.getOptions().get( 0 ) );
        Assert.assertSame( option2, optionSet.getOptions().get( 1 ) );
        Assert.assertSame( option3, optionSet.getOptions().get( 2 ) );
    }

    @Test
    public void addOptionWithBetween()
    {
        final OptionSet optionSet = new OptionSet();
        final Option option1 = new Option();
        option1.setSortOrder( 8 );
        final Option option2 = new Option();
        option2.setSortOrder( 10 );
        final Option option3 = new Option();
        option3.setSortOrder( 9 );

        optionSet.addOption( option1 );
        optionSet.addOption( option2 );
        optionSet.addOption( option3 );

        Assert.assertEquals( 3, optionSet.getOptions().size() );
        Assert.assertSame( option1, optionSet.getOptions().get( 0 ) );
        Assert.assertSame( option2, optionSet.getOptions().get( 2 ) );
        Assert.assertSame( option3, optionSet.getOptions().get( 1 ) );
    }
}