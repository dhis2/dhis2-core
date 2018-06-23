package org.hisp.dhis.icon;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Kristian Wærstad
 */
public class DefaultIconService
    implements IconService
{
    private Map<String, IconData> icons = Arrays.stream( Icon.values() )
        .map( Icon::getVariants )
        .flatMap( Collection::stream )
        .collect( Collectors.toMap( IconData::getKey, Function.identity() ) );

    @Override
    public Collection<IconData> getIcons()
    {
        return icons.values();
    }

    @Override
    public Collection<IconData> getIcons( Collection<String> keywords )
    {
        return icons.values().stream()
            .filter( icon -> Arrays.asList( icon.getKeywords() ).containsAll( keywords ) )
            .collect( Collectors.toList() );
    }

    @Override
    public Optional<IconData> getIcon( String key )
    {
        return Optional.ofNullable( icons.get( key ) );
    }

    @Override
    public Collection<String> getKeywords()
    {
        return icons.values().stream()
            .map( IconData::getKeywords )
            .flatMap( Arrays::stream )
            .collect( Collectors.toSet() );
    }
}
