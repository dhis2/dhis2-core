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
package org.hisp.dhis.webapi.json;

import static java.lang.Character.toChars;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link JsonDocument} is a JSON parser specifically designed as a verifying
 * tool for JSON trees which skips though JSON trees to extract only a specific
 * {@link JsonNode} at a certain path while have subsequent operations benefit
 * from partial parsing work already done in previous calls.
 *
 * Supported paths:
 *
 * <pre>
 * $ = root
 * $.property = field property in root object
 * $.a.b = field b in a in root object
 * $[0] = index 0 of root array
 * $.a[1] index 1 of array in a field in root
 * </pre>
 *
 * This parser follows the JSON standard as defined by
 * <a href="https://www.json.org/">json.org</a>.
 *
 * @author Jan Bernitt
 */
public final class JsonDocument implements Serializable
{
    /**
     * Thrown when the JSON content turns out to be invalid JSON.
     */
    public static class JsonFormatException extends IllegalArgumentException
    {
        public JsonFormatException( String message )
        {
            super( message );
        }

        public JsonFormatException( char[] json, int index, char expected )
        {
            this( createParseErrorMessage( json, index, expected ) );
        }

        private static String createParseErrorMessage( char[] json, int index, char expected )
        {
            int start = max( 0, index - 20 );
            int length = min( json.length - start, 40 );
            String section = new String( json, start, length );
            char[] pointer = new char[index - start + 1];
            Arrays.fill( pointer, ' ' );
            pointer[pointer.length - 1] = '^';
            String expectedText = expected == '~' ? "start of value" : "`" + expected + "`";
            return String.format( "Unexpected character at position %d,%n%s%n%s expected %s",
                index, section, new String( pointer ), expectedText );
        }
    }

    /**
     * Exception thrown when the path given to {@link JsonDocument#get(String)}
     * does not exist.
     */
    public static class JsonPathException extends IllegalArgumentException
    {
        public JsonPathException( String s )
        {
            super( s );
        }
    }

    /**
     * Possible types of JSON nodes in {@link JsonNode} tree.
     */
    public enum JsonNodeType
    {
        OBJECT,
        ARRAY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }

    /**
     * API of a JSON tree as it actually exist.
     *
     * Operations are lazily evaluated to make working with the JSON tree
     * efficient.
     */
    public interface JsonNode extends Serializable
    {
        /**
         * @return the type of the node as derived from the node beginning
         */
        JsonNodeType getType();

        /**
         * @return path within the overall content this node represents
         */
        String getPath();

        /**
         * @return the plain JSON of this node as defined in the overall content
         */
        String getDeclaration();

        /**
         * @return number of elements in an array or number of fields in an
         *         object, otherwise undefined
         * @throws UnsupportedOperationException when this node in neither an
         *         array nor an object
         */
        default int size()
        {
            throw new UnsupportedOperationException( getType() + " node has no size property." );
        }

        /**
         * @return true if an array or object has no elements/fields, otherwise
         *         undefined
         * @throws UnsupportedOperationException when this node in neither an
         *         array nor an object
         */
        default boolean isEmpty()
        {
            throw new UnsupportedOperationException( getType() + " node has no empty property." );
        }

        /**
         * The value depends on the {@link #getType()}:
         * <ul>
         * <li>{@link JsonNodeType#NULL} returns {@code null}</li>
         * <li>{@link JsonNodeType#BOOLEAN} returns {@link Boolean}</li>
         * <li>{@link JsonNodeType#STRING} returns {@link String}</li>
         * <li>{@link JsonNodeType#NUMBER} returns either {@link Integer},
         * {@link Long} or {@link Double}</li>
         * <li>{@link JsonNodeType#ARRAY} returns an {@link java.util.List} of
         * {@link JsonNode}</li>
         * <li>{@link JsonNodeType#ARRAY} returns a {@link Map} or
         * {@link String} keys and {@link JsonNode} values</li>
         * </ul>
         *
         * @return the nodes values as described in the above table
         */
        Serializable value();

        /**
         * @return this {@link #value()} as as {@link Map} (only defined when
         *         this node is of type {@link JsonNodeType#OBJECT}).
         */
        @SuppressWarnings( "unchecked" )
        default Map<String, JsonNode> object()
        {
            return (Map<String, JsonNode>) value();
        }

        /**
         * @return this {@link #value()} as as {@link List} (only defined when
         *         this node is of type {@link JsonNodeType#ARRAY}).
         */
        @SuppressWarnings( "unchecked" )
        default List<JsonNode> array()
        {
            return (List<JsonNode>) value();
        }

        /**
         * @return offset or index in the overall content where this node starts
         *         (inclusive, points to first index that belongs to the node)
         */
        int startIndex();

        /**
         * @return offset or index in the overall content where this node ends
         *         (exclusive, points to first index after the node)
         */
        int endIndex();
    }

    /**
     * The main idea of lazy nodes is that at creation only the start index and
     * the path the node represents is known.
     *
     * The "expensive" operations to access the nodes {@link #value()} or find
     * its {@link #endIndex()} are only computed on demand.
     */
    private abstract static class LazyJsonNode implements JsonNode
    {
        final String path;

        final char[] json;

        final int start;

        private Integer end;

        private Serializable value;

        LazyJsonNode( String path, char[] json, int start )
        {
            this.path = path;
            this.json = json;
            this.start = start;
        }

        @Override
        public final String getPath()
        {
            return path;
        }

        @Override
        public final String getDeclaration()
        {
            int endIndex = endIndex();
            return new String( json, start, endIndex - start );
        }

        @Override
        public final Serializable value()
        {
            if ( getType() == JsonNodeType.NULL )
            {
                return null;
            }
            if ( value == null )
            {
                value = parseValue();
            }
            return value;
        }

        @Override
        public final int startIndex()
        {
            return start;
        }

        @Override
        public final int endIndex()
        {
            if ( end == null )
            {
                end = skipNodeAutodetect( json, start );
            }
            return end;
        }

        @Override
        public final String toString()
        {
            return getDeclaration();
        }

        /**
         * @return parses the JSON to a value as described by {@link #value()}
         */
        abstract Serializable parseValue();

        static JsonNode autoDetect( String path, char[] json, int atIndex, Map<String, JsonNode> nodesByPath )
        {
            JsonNode node = nodesByPath.get( path );
            if ( node != null )
            {
                return node;
            }
            switch ( json[atIndex] )
            {
            case '{': // object node
                return new LazyJsonObject( path, json, atIndex, nodesByPath );
            case '[': // array node
                return new LazyJsonArray( path, json, atIndex, nodesByPath );
            case '"': // string node
                return new LazyJsonString( path, json, atIndex );
            case 't': // true => boolean node
            case 'f': // false => boolean node
                return new LazyJsonBoolean( path, json, atIndex );
            case 'n': // null node
                return new LazyJsonNull( path, json, atIndex );
            default: // must be number node then...
                return new LazyJsonNumber( path, json, atIndex );
            }
        }
    }

    private static final class LazyJsonObject extends LazyJsonNode
    {
        private final Map<String, JsonNode> nodesByPath;

        LazyJsonObject( String path, char[] json, int start, Map<String, JsonNode> nodesByPath )
        {
            super( path, json, start );
            this.nodesByPath = nodesByPath;
        }

        @Override
        public JsonNodeType getType()
        {
            return JsonNodeType.OBJECT;
        }

        @Override
        public boolean isEmpty()
        {
            // avoid computing value with a "cheap" check
            return json[skipWhitespace( json, start + 1 )] == '}';
        }

        @Override
        public int size()
        {
            // only compute value when needed
            return isEmpty() ? 0 : object().size();
        }

        @Override
        Serializable parseValue()
        {
            LinkedHashMap<String, JsonNode> object = new LinkedHashMap<>();
            int index = expectChar( json, start, '{' );
            index = skipWhitespace( json, index );
            while ( index < json.length && json[index] != '}' )
            {
                LazyJsonString property = new LazyJsonString( path + ".?", json, index );
                String fieldName = String.valueOf( property.value() );
                String fPath = path + "." + fieldName;
                index = expectSeparator( json, property.endIndex(), ':' );
                int fStart = index;
                JsonNode field = nodesByPath.computeIfAbsent( fPath,
                    key -> autoDetect( key, json, fStart, nodesByPath ) );
                object.put( fieldName, field );
                index = field.endIndex();
                index = skipSeparator( json, index, ',' );
            }
            expectChar( json, index, '}' );
            return object;
        }
    }

    private static final class LazyJsonArray extends LazyJsonNode
    {
        private final Map<String, JsonNode> nodesByPath;

        LazyJsonArray( String path, char[] json, int start, Map<String, JsonNode> nodesByPath )
        {
            super( path, json, start );
            this.nodesByPath = nodesByPath;
        }

        @Override
        public JsonNodeType getType()
        {
            return JsonNodeType.ARRAY;
        }

        @Override
        public boolean isEmpty()
        {
            // avoid computing value with a "cheap" check
            return json[skipWhitespace( json, start + 1 )] == ']';
        }

        @Override
        public int size()
        {
            // only compute value when needed
            return isEmpty() ? 0 : array().size();
        }

        @Override
        Serializable parseValue()
        {
            ArrayList<JsonNode> array = new ArrayList<>();
            int index = expectChar( json, start, '[' );
            index = skipWhitespace( json, index );
            while ( index < json.length && json[index] != ']' )
            {
                String ePath = path + '[' + array.size() + "]";
                int eStart = index;
                JsonNode e = nodesByPath.computeIfAbsent( ePath,
                    key -> autoDetect( key, json, eStart, nodesByPath ) );
                array.add( e );
                index = e.endIndex();
                index = skipSeparator( json, index, ',' );
            }
            expectChar( json, index, ']' );
            return array;
        }
    }

    private static final class LazyJsonNumber extends LazyJsonNode
    {

        LazyJsonNumber( String path, char[] json, int start )
        {
            super( path, json, start );
        }

        @Override
        public JsonNodeType getType()
        {
            return JsonNodeType.NUMBER;
        }

        @Override
        Number parseValue()
        {
            int end = skipNumber( json, start );
            double number = Double.parseDouble( new String( json, start, end - start ) );
            if ( number % 1 == 0d )
            {
                long asLong = (long) number;
                if ( asLong < Integer.MAX_VALUE && asLong > Integer.MIN_VALUE )
                {
                    return (int) asLong;
                }
                return asLong;
            }
            return number;
        }

    }

    private static final class LazyJsonString extends LazyJsonNode
    {

        LazyJsonString( String path, char[] json, int start )
        {
            super( path, json, start );
        }

        @Override
        public JsonNodeType getType()
        {
            return JsonNodeType.STRING;
        }

        @Override
        String parseValue()
        {
            StringBuilder str = new StringBuilder();
            int index = start;
            index = expectChar( json, index, '"' );
            while ( index < json.length )
            {
                char c = json[index++];
                if ( c == '"' )
                {
                    // found the end (if escaped we would have hopped over)
                    return str.toString();
                }
                if ( c == '\\' )
                {
                    switch ( json[index++] )
                    {
                    case 'u': // unicode uXXXX
                        str.append( toChars( parseInt( new String( json, index, 4 ), 16 ) ) );
                        index += 4; // u we already skipped
                        break;
                    case '\\':
                        str.append( '\\' );
                        break;
                    case '/':
                        str.append( '/' );
                        break;
                    case 'b':
                        str.append( '\b' );
                        break;
                    case 'f':
                        str.append( '\f' );
                        break;
                    case 'n':
                        str.append( '\n' );
                        break;
                    case 'r':
                        str.append( '\r' );
                        break;
                    case 't':
                        str.append( '\t' );
                        break;
                    default:
                        throw new JsonFormatException( json, index, '?' );
                    }
                }
                else
                {
                    str.append( c );
                }
            }
            // throws...
            expectChar( json, index, '"' );
            return null;
        }
    }

    private static final class LazyJsonBoolean extends LazyJsonNode
    {

        LazyJsonBoolean( String path, char[] json, int start )
        {
            super( path, json, start );
        }

        @Override
        public JsonNodeType getType()
        {
            return JsonNodeType.BOOLEAN;
        }

        @Override
        Boolean parseValue()
        {
            return skipBoolean( json, start ) == start + 4; // the it was true
        }

    }

    private static final class LazyJsonNull extends LazyJsonNode
    {

        LazyJsonNull( String path, char[] json, int start )
        {
            super( path, json, start );
        }

        @Override
        public JsonNodeType getType()
        {
            return JsonNodeType.NULL;
        }

        @Override
        Serializable parseValue()
        {
            skipBoolean( json, start );
            return null;
        }
    }

    private final char[] json;

    private final Map<String, JsonNode> nodesByPath = new HashMap<>();

    JsonDocument( String json )
    {
        this.json = json.toCharArray();
        nodesByPath.put( "", LazyJsonNode.autoDetect( "", this.json, skipWhitespace( this.json, 0 ), nodesByPath ) );
    }

    @Override
    public String toString()
    {
        return new String( json );
    }

    /**
     * Returns the {@link JsonNode} for the given path.
     *
     * @param path a path as described in {@link JsonDocument} javadoc
     * @return the {@link JsonNode} for the path, never {@code null}
     * @throws JsonPathException when this document does not contain a node
     *         corresponding to the given path or the given path is not a valid
     *         path expression
     * @throws JsonFormatException when this document contains malformed JSON
     *         that confuses the parser
     */
    public JsonNode get( String path )
    {
        if ( path.startsWith( "$" ) )
        {
            path = path.substring( 1 );
        }
        JsonNode node = nodesByPath.get( path );
        if ( node != null )
        {
            return node;
        }
        JsonNode parent = getClosestIndexedParent( path );
        String pathToGo = path.substring( parent.getPath().length() );
        while ( !pathToGo.isEmpty() )
        {
            if ( pathToGo.startsWith( "[" ) )
            {
                checkNodeIs( parent, JsonNodeType.ARRAY, path );
                List<JsonNode> array = parent.array();
                int index = parseInt( pathToGo.substring( 1, pathToGo.indexOf( ']' ) ) );
                checkIndexExists( parent, array, index, path );
                parent = array.get( index );
                pathToGo = pathToGo.substring( pathToGo.indexOf( ']' ) + 1 );
            }
            else if ( pathToGo.startsWith( "." ) )
            {
                checkNodeIs( parent, JsonNodeType.OBJECT, path );
                Map<String, JsonNode> object = parent.object();
                String property = getHeadProperty( pathToGo );
                checkFieldExists( parent, object, property, path );
                parent = object.get( property );
                pathToGo = pathToGo.substring( 1 + property.length() );
            }
            else
            {
                throw new JsonPathException( String.format( "Malformed path %s at %s.", path, pathToGo ) );
            }
        }
        return parent;
    }

    private String getHeadProperty( String path )
    {
        int index = 1;
        while ( index < path.length() && path.charAt( index ) != '.' && path.charAt( index ) != '[' )
        {
            index++;
        }
        return path.substring( 1, index );
    }

    private void checkFieldExists( JsonNode parent, Map<String, JsonNode> object, String property, String path )
    {
        if ( !object.containsKey( property ) )
        {
            throw new JsonPathException(
                String.format( "Path `%s` does not exist, object `%s` does not have a property `%s`", path,
                    parent.getPath(), property ) );
        }
    }

    private void checkIndexExists( JsonNode parent, List<JsonNode> array, int index, String path )
    {
        if ( index > array.size() )
        {
            throw new JsonPathException(
                String.format( "Path `%s` does not exist, array `%s` has only `%d` elements.", path, parent.getPath(),
                    array.size() ) );
        }
    }

    private void checkNodeIs( JsonNode parent, JsonNodeType expected, String path )
    {
        if ( parent.getType() != expected )
        {
            throw new JsonPathException(
                String.format( "Path `%s` does not exist, parent `%s` is not an %s but a %s node.", path,
                    parent.getPath(), expected, parent.getType() ) );
        }
    }

    private JsonNode getClosestIndexedParent( String path )
    {
        String parentPath = getParentPath( path );
        JsonNode parent = nodesByPath.get( parentPath );
        if ( parent != null )
        {
            return parent;
        }
        return getClosestIndexedParent( parentPath );
    }

    private String getParentPath( String path )
    {
        if ( path.endsWith( "]" ) )
        {
            return path.substring( 0, path.lastIndexOf( '[' ) );
        }
        int end = path.lastIndexOf( '.' );
        return end < 0 ? "" : path.substring( 0, end );
    }

    /*--------------------------------------------------------------------------
     Parsing support
     -------------------------------------------------------------------------*/

    @FunctionalInterface
    interface CharPredicate
    {
        boolean test( char c );
    }

    static int skipNodeAutodetect( char[] json, int atIndex )
    {
        switch ( json[atIndex] )
        {
        case '{': // object node
            return skipObject( json, atIndex );
        case '[': // array node
            return skipArray( json, atIndex );
        case '"': // string node
            return skipString( json, atIndex );
        case 't': // true => boolean node
        case 'f': // false => boolean node
            return skipBoolean( json, atIndex );
        case 'n': // null node
            return skipNull( json, atIndex );
        default: // must be number node then...
            return skipNumber( json, atIndex );
        }
    }

    static int skipObject( char[] json, int fromIndex )
    {
        int index = fromIndex;
        index = expectChar( json, index, '{' );
        index = skipWhitespace( json, index );
        while ( index < json.length && json[index] != '}' )
        {
            index = skipString( json, index );
            index = expectSeparator( json, index, ':' );
            index = skipNodeAutodetect( json, index );
            index = skipSeparator( json, index, ',' );
        }
        return expectChar( json, index, '}' );
    }

    static int skipArray( char[] json, int fromIndex )
    {
        int index = fromIndex;
        index = expectChar( json, index, '[' );
        index = skipWhitespace( json, index );
        while ( index < json.length && json[index] != ']' )
        {
            index = skipNodeAutodetect( json, index );
            index = skipSeparator( json, index, ',' );
        }
        return expectChar( json, index, ']' );
    }

    private static int expectSeparator( char[] json, int index, char separator )
    {
        return skipWhitespace( json, expectChar( json, skipWhitespace( json, index ), separator ) );
    }

    private static int skipSeparator( char[] json, int index, char separator )
    {
        index = skipWhitespace( json, index );
        return json[index] == separator ? skipWhitespace( json, index + 1 ) : index;
    }

    private static int skipBoolean( char[] json, int fromIndex )
    {
        return expectChars( json, fromIndex, json[fromIndex] == 't' ? "true" : "false" );
    }

    private static int skipNull( char[] json, int fromIndex )
    {
        return expectChars( json, fromIndex, "null" );
    }

    private static int skipString( char[] json, int fromIndex )
    {
        int index = fromIndex;
        index = expectChar( json, index, '"' );
        while ( index < json.length )
        {
            char c = json[index++];
            if ( c == '"' )
            {
                // found the end (if escaped we would have hopped over)
                return index;
            }
            if ( c == '\\' )
            {
                // hop over escaped char or unicode
                index += json[index] == 'u' ? 5 : 1;
            }
        }
        return expectChar( json, index, '"' );
    }

    private static int skipNumber( char[] json, int fromIndex )
    {
        int index = fromIndex;
        index = skipChar( json, index, '-' );
        index = expectChar( json, index, JsonDocument::isDigit );
        index = skipDigits( json, index );
        index = skipChar( json, index, '.' );
        index = skipDigits( json, index );
        index = skipChar( json, index, 'e', 'E' );
        index = skipChar( json, index, '+', '-' );
        return skipDigits( json, index );
    }

    private static int skipWhitespace( char[] json, int fromIndex )
    {
        return skipWhile( json, fromIndex, JsonDocument::isWhitespace );
    }

    private static int skipDigits( char[] json, int fromIndex )
    {
        return skipWhile( json, fromIndex, JsonDocument::isDigit );
    }

    /**
     * JSON only considers ASCII digits as digits
     */
    private static boolean isDigit( char c )
    {
        return c >= '0' && c <= '9';
    }

    /**
     * JSON only considers ASCII whitespace as whitespace
     */
    private static boolean isWhitespace( char c )
    {
        return c == ' ' || c == '\n' || c == '\t' || c == '\r';
    }

    private static int skipWhile( char[] json, int fromIndex, CharPredicate whileTrue )
    {
        int index = fromIndex;
        while ( index < json.length && whileTrue.test( json[index] ) )
        {
            index++;
        }
        return index;
    }

    private static int skipChar( char[] json, int index, char c )
    {
        return index < json.length && json[index] == c ? index + 1 : index;
    }

    private static int skipChar( char[] json, int index, char... anyOf )
    {
        if ( index >= json.length )
        {
            return index;
        }
        for ( char c : anyOf )
        {
            if ( json[index] == c )
            {
                return index + 1;
            }
        }
        return index;
    }

    private static int expectChars( char[] json, int index, CharSequence expected )
    {
        int length = expected.length();
        for ( int i = 0; i < length; i++ )
        {
            expectChar( json, index + i, expected.charAt( i ) );
        }
        return index + length;
    }

    private static int expectChar( char[] json, int index, CharPredicate expected )
    {
        if ( index >= json.length )
        {
            throw new JsonFormatException( "Expected character but reached EOI: " + getEndSection( json, index ) );
        }
        if ( !expected.test( json[index] ) )
        {
            throw new JsonFormatException( json, index, '~' );
        }
        return index + 1;
    }

    private static int expectChar( char[] json, int index, char expected )
    {
        if ( index >= json.length )
        {
            throw new JsonFormatException( "Expected " + expected + " but reach EOI: " + getEndSection( json, index ) );
        }
        if ( json[index] != expected )
        {
            throw new JsonFormatException( json, index, expected );
        }
        return index + 1;
    }

    private static String getEndSection( char[] json, int index )
    {
        return new String( json, max( 0, min( json.length, index ) - 20 ), min( 20, json.length ) );
    }

}
