/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common.query;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.firstNonBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.analytics.common.query.QuotingUtils.doubleQuote;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;

/**
 * This class represents a {@link Renderable} field. It's mainly used for SQL
 * query rendering and headers display.
 */
@RequiredArgsConstructor( staticName = "of", access = AccessLevel.PRIVATE )
public class Field extends BaseRenderable
{
    private final String tableAlias;

    private final Renderable name;

    @Getter
    private final String fieldAlias;

    @Getter
    private final DimensionIdentifier<DimensionParam> dimensionIdentifier;

    private final Boolean quotingNeeded;

    /**
     * Static constructor for a field which double quote "name" when rendered.
     *
     * @param tableAlias the table alias.
     * @param name the {@link Renderable} name of the field.
     * @param fieldAlias the alias of the field.
     * @return a new {@link Field} instance.
     */
    public static Field of( String tableAlias, Renderable name, String fieldAlias )
    {
        return of( tableAlias, name, fieldAlias, DimensionIdentifier.EMPTY, true );
    }

    /**
     * Static constructor for a field which double quote "name" when rendered.
     *
     * @param tableAlias the table alias.
     * @param name the {@link Renderable} name of the field.
     * @return a new {@link Field} instance.
     */
    public static Field of( String tableAlias, Renderable name,
        DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return of( tableAlias, name, EMPTY, dimensionIdentifier, true );
    }

    /**
     * Static constructor for a field which will not double quote "name" when
     * rendered.
     *
     * @param tableAlias the table alias.
     * @param name the {@link Renderable} name of the field.
     * @param fieldAlias the alias of the field.
     * @return a new {@link Field} instance.
     */
    public static Field ofUnquoted( String tableAlias, Renderable name, String fieldAlias )
    {
        return of( tableAlias, name, fieldAlias, DimensionIdentifier.EMPTY, false );
    }

    /**
     * Static constructor for a field which will not double quote "name" when
     * rendered.
     *
     * @param tableAlias the table alias.
     * @param name the {@link Renderable} name of the field.
     * @param dimensionIdentifier the alias of the field.
     * @return a new {@link Field} instance.
     */
    public static Field ofUnquoted( String tableAlias, Renderable name,
        DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return of( tableAlias, name, EMPTY, dimensionIdentifier, false );
    }

    /**
     * Simplest field renderer that takes into consideration only the name.
     *
     * @param name the {@link Renderable} name of the field.
     * @return a new {@link Field} instance.
     */
    public static Field of( String name )
    {
        return of( EMPTY, () -> name, EMPTY, DimensionIdentifier.EMPTY, true );
    }

    public String getDimensionIdentifier()
    {
        return firstNonBlank( dimensionIdentifier.getKey(), fieldAlias );
    }

    @Override
    public String render()
    {
        String rendered = EMPTY;

        if ( isNotBlank( tableAlias ) )
        {
            rendered = tableAlias + ".";
        }

        if ( toBooleanDefaultIfNull( quotingNeeded, false ) )
        {
            rendered = rendered + doubleQuote( name.render() );
        }
        else
        {
            rendered = rendered + name.render();
        }

        if ( isNotBlank( getDimensionIdentifier() ) )
        {
            rendered = rendered + " as " + doubleQuote( getDimensionIdentifier() );
        }

        return rendered;
    }
}
