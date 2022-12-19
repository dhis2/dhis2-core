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
package org.hisp.dhis.webapi.openapi;

import java.util.Collection;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * A utility to generate JSON based on simple {@link Runnable} callbacks for
 * nesting of arrays and objects.
 *
 * @author Jan Bernitt
 */
@RequiredArgsConstructor
public class JsonGenerator
{
    @Value
    static class Format
    {
        public static final Format PRETTY_PRINT = new Format( true, true, true, true, true, "  " );

        public static final Format SMALL = new Format( false, false, false, false, false, "" );

        boolean newLineAfterMember;

        boolean newLineAfterObjectStart;

        boolean newLineBeforeObjectEnd;

        boolean newLineAfterArrayStart;

        boolean newLineBeforeArrayEnd;

        String memberIndent;
    }

    private final StringBuilder out;

    private final Format format;

    private String indent = "";

    @Override
    public final String toString()
    {
        return out.toString();
    }

    final void addRootObject( Runnable addMembers )
    {
        addObjectMember( null, addMembers );
        discardLastMemberComma( 0 );
    }

    final void addObjectMember( String name, boolean condition, Runnable addMembers )
    {
        if ( condition )
        {
            addObjectMember( name, addMembers );
        }
    }

    final <E> void addObjectMember( String name, Collection<E> members, Consumer<E> forEach )
    {
        if ( !members.isEmpty() )
        {
            addObjectMember( name, () -> members.forEach( forEach ) );
        }
    }

    final void addObjectMember( String name, Runnable addMembers )
    {
        appendMemberName( name );
        out.append( "{" );
        if ( format.isNewLineAfterObjectStart() )
            out.append( '\n' );
        int length = out.length();
        if ( format.isNewLineAfterMember() )
            indent += format.getMemberIndent();
        appendItems( addMembers );
        if ( format.isNewLineAfterMember() )
            indent = indent.substring( 0, indent.length() - format.getMemberIndent().length() );
        discardLastMemberComma( length );
        if ( format.isNewLineBeforeObjectEnd() )
        {
            out.append( '\n' );
            appendMemberIndent();
        }
        out.append( "}" );
        appendMemberComma();
    }

    final void addArrayMember( String name, Collection<String> values )
    {
        addArrayMember( name, values, value -> addStringMember( null, value ) );
    }

    final <E> void addArrayMember( String name, Collection<E> items, Consumer<E> forEach )
    {
        if ( !items.isEmpty() )
        {
            addArrayMember( name, () -> items.forEach( forEach ) );
        }
    }

    final void addArrayMember( String name, Runnable addElements )
    {
        appendMemberName( name );
        out.append( '[' );
        if ( format.isNewLineAfterArrayStart() )
            out.append( '\n' );
        int length = out.length();
        appendItems( addElements );
        discardLastMemberComma( length );
        if ( format.isNewLineBeforeArrayEnd() )
        {
            out.append( '\n' );
            appendMemberIndent();
        }
        out.append( ']' );
        appendMemberComma();
    }

    final void addStringMember( String name, String value )
    {
        if ( value != null )
        {
            appendMemberName( name );
            appendString( value );
            appendMemberComma();
        }
    }

    final void addBooleanMember( String name, Boolean value )
    {
        if ( value != null )
        {
            addBooleanMember( name, value.booleanValue() );
        }
    }

    final void addBooleanMember( String name, boolean value )
    {
        appendMemberName( name );
        out.append( value ? "true" : "false" );
        appendMemberComma();
    }

    final void addNumberMember( String name, Integer value )
    {
        if ( value != null )
        {
            addNumberMember( name, value.intValue() );
        }
    }

    final void addNumberMember( String name, int value )
    {
        appendMemberName( name );
        out.append( value );
        appendMemberComma();
    }

    private void appendItems( Runnable items )
    {
        if ( items != null )
        {
            items.run();
        }
    }

    private StringBuilder appendString( String str )
    {
        return str == null
            ? out.append( "null" )
            : out.append( '"' ).append( str.replace( "\"", "\\\"" ) ).append( '"' );
    }

    private void appendMemberName( String name )
    {
        appendMemberIndent();
        if ( name != null )
        {
            appendString( name ).append( ':' );
        }
    }

    private void appendMemberIndent()
    {
        if ( format.isNewLineAfterMember() )
            out.append( indent );
    }

    private void appendMemberComma()
    {
        out.append( ',' );
        if ( format.isNewLineAfterMember() )
            out.append( '\n' );
    }

    private void discardLastMemberComma( int length )
    {
        if ( out.length() > length )
        {
            int back = format.isNewLineAfterMember() ? 2 : 1;
            out.setLength( out.length() - back ); // discard last ,
        }
    }
}
