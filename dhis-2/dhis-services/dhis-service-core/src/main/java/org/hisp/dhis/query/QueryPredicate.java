package org.hisp.dhis.query;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hisp.dhis.query.operators.MatchMode;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.util.DateUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.function.Function;

public class QueryPredicate<T extends Comparable<? extends T>, Y>
{
    private Root<Y> root;
    private CriteriaBuilder builder;
    private String path;
    private Object argValue;
    private Schema schema;
    private String operator;
    private Class<T> klass;
    private Class<? extends T> propertyKlass;

    private Function<Root<Y>, Predicate> getPredicate()
    {
        switch ( operator )
        {
        case "eq":
        {
            return root -> builder.equal( root.get( path ), parseValue( propertyKlass ) );
        }
        case "!eq":
        {
            return root -> builder.notEqual( root.get( path ), parseValue( propertyKlass, argValue ) );
        }
        case "ne":
        {
            return root -> builder.notEqual( root.get( path ), parseValue( propertyKlass, argValue ) );
        }
        case "neq":
        {
            return root -> builder.notEqual( root.get( path ), parseValue( propertyKlass, argValue ) );
        }
        case "gt":
        {
            return root -> builder.greaterThan( root.get( path ).as( propertyKlass ), QueryUtils.parseValue( propertyKlass, argValue ) );
        }
        case "lt":
        {
            return Restrictions.lt( path, QueryUtils.parseValue( property.getKlass(), arg ) );
        }
        case "gte":
        {
            return Restrictions.ge( path, QueryUtils.parseValue( property.getKlass(), arg ) );
        }
        case "ge":
        {
            return Restrictions.ge( path, QueryUtils.parseValue( property.getKlass(), arg ) );
        }
        case "lte":
        {
            return Restrictions.le( path, QueryUtils.parseValue( property.getKlass(), arg ) );
        }
        case "le":
        {
            return Restrictions.le( path, QueryUtils.parseValue( property.getKlass(), arg ) );
        }
        case "like":
        {
            return Restrictions.like( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.ANYWHERE );
        }
        case "!like":
        {
            return Restrictions.notLike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.ANYWHERE );
        }
        case "$like":
        {
            return Restrictions.like( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
        }
        case "!$like":
        {
            return Restrictions.notLike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
        }
        case "like$":
        {
            return Restrictions.like( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.END );
        }
        case "!like$":
        {
            return Restrictions.notLike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.END );
        }
        case "ilike":
        {
            return Restrictions.ilike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.ANYWHERE );
        }
        case "!ilike":
        {
            return Restrictions.notIlike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.ANYWHERE );
        }
        case "startsWith":
        case "$ilike":
        {
            return Restrictions.ilike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
        }
        case "!$ilike":
        {
            return Restrictions.notIlike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
        }
        case "token":
        {
            return Restrictions.token( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
        }
        case "!token":
        {
            return Restrictions.notToken( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.START );
        }
        case "endsWith":
        case "ilike$":
        {
            return Restrictions.ilike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.END );
        }
        case "!ilike$":
        {
            return Restrictions.notIlike( path, QueryUtils.parseValue( property.getKlass(), arg ), MatchMode.END );
        }
        case "in":
        {
            Collection<?> values = null;

            if ( property.isCollection() )
            {
                values = QueryUtils.parseValue( Collection.class, property.getItemKlass(), arg );
            }
            else
            {
                values = QueryUtils.parseValue( Collection.class, property.getKlass(), arg );
            }

            if ( values == null || values.isEmpty() )
            {
                throw new QueryParserException( "Invalid argument `" + arg + "` for in operator." );
            }

            return Restrictions.in( path, values );
        }
        case "!in":
        {
            Collection<?> values = null;

            if ( property.isCollection() )
            {
                values = QueryUtils.parseValue( Collection.class, property.getItemKlass(), arg );
            }
            else
            {
                values = QueryUtils.parseValue( Collection.class, property.getKlass(), arg );
            }

            if ( values == null || values.isEmpty() )
            {
                throw new QueryParserException( "Invalid argument `" + arg + "` for in operator." );
            }

            return Restrictions.notIn( path, values );
        }
        case "null":
        {
            return Restrictions.isNull( path );
        }
        case "!null":
        {
            return Restrictions.isNotNull( path );
        }
        case "empty":
        {
            return Restrictions.isEmpty( path );
        }
        default:
        {
            throw new QueryParserException( "`" + operator + "` is not a valid operator." );
        }
        }
    }

    public  T parseValue( Object objectValue )
    {
        if ( klass.isInstance( objectValue ) )
        {
            return (T) objectValue;
        }

        if ( !String.class.isInstance( objectValue ) )
        {
            return (T) objectValue;
        }
        return parseValue( null, ( String ) objectValue );
    }

    @SuppressWarnings( "unchecked" )
    private T parseValue( Class<?> secondaryKlass, String value )
    {

        if ( Integer.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Integer.valueOf( value );
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Integer`." );
            }
        }
        else if ( Boolean.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Boolean.valueOf( value );
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Boolean`." );
            }
        }
        else if ( Float.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Float.valueOf( value );
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Float`." );
            }
        }
        else if ( Double.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Double.valueOf( value );
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Double`." );
            }
        }
        else if ( Date.class.isAssignableFrom( klass ) )
        {
            try
            {
                Date date = DateUtils.parseDate( value );
                return (T) date;
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Date`." );
            }
        }
        else if ( Enum.class.isAssignableFrom( klass ) )
        {
            T enumValue = getEnumValue( klass, value );

            if ( enumValue != null )
            {
                return enumValue;
            }
        }
        else if ( Collection.class.isAssignableFrom( klass ) )
        {
            if ( !value.startsWith( "[" ) || !value.endsWith( "]" ) )
            {
                try
                {
                    return (T) Integer.valueOf( value );
                }
                catch ( NumberFormatException e )
                {
                    throw new QueryParserException( "Collection size must be integer `" + value + "`" );
                }
            }

            String[] split = value.substring( 1, value.length() - 1 ).split( "," );
            List<String> items = Lists.newArrayList( split );

            if ( secondaryKlass != null )
            {
                List<Object> convertedList = new ArrayList<>();

                for ( String item : items )
                {
                    Object convertedValue = parseValue( secondaryKlass, null, item );

                    if ( convertedValue != null )
                    {
                        convertedList.add( convertedValue );
                    }
                }

                return (T) convertedList;
            }

            return (T) items;
        }

        throw new QueryParserException( "Unable to parse `" + value + "` to `" + klass.getSimpleName() + "`." );
    }

    /**
     * Try and parse `value` as Enum. Throws `QueryException` if invalid value.
     *
     * @param klass the Enum class.
     * @param value the enum value.
     */
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public static <T> T getEnumValue( Class<T> klass, String value )
    {
        Optional<? extends Enum<?>> enumValue = Enums.getIfPresent( (Class<? extends Enum>) klass, value );

        if ( enumValue.isPresent() )
        {
            return (T) enumValue.get();
        }
        else
        {
            Object[] possibleValues = klass.getEnumConstants();
            throw new QueryParserException( "Unable to parse `" + value + "` as `" + klass + "`, available values are: " + Arrays
                .toString( possibleValues ) );
        }
    }

    public static Object parseValue( String value )
    {
        if ( value == null || StringUtils.isEmpty( value ) )
        {
            return null;
        }
        else if ( NumberUtils.isNumber( value ) )
        {
            return value;
        }
        else
        {
            return "'" + value + "'";
        }
    }

    /**
     * Convert a List of select fields into a string as in SQL select query.
     * <p>
     * If input is null, return "*" means the query will select all fields.
     *
     * @param fields list of fields in a select query.
     * @return a string which is concatenated of list fields, separate by comma.
     */
    public static String parseSelectFields( List<String> fields )
    {
        if ( fields == null || fields.isEmpty() )
        {
            return " * ";
        }
        else
        {
            String str = StringUtils.EMPTY;
            for ( int i = 0; i < fields.size(); i++ )
            {
                str += fields.get( i );
                if ( i < fields.size() - 1 )
                {
                    str += ",";
                }
            }
            return str;
        }
    }


    /**
     * Converts a String with JSON format [x,y,z] into an SQL query collection format (x,y,z).
     *
     * @param value a string contains a collection with JSON format [x,y,z].
     * @return a string contains a collection with SQL query format (x,y,z).
     */
    public static String convertCollectionValue( String value )
    {
        if ( StringUtils.isEmpty( value ) )
        {
            throw new QueryParserException( "Value is null" );
        }

        if ( !value.startsWith( "[" ) || !value.endsWith( "]" ) )
        {
            throw new QueryParserException( "Invalid query value" );
        }

        String[] split = value.substring( 1, value.length() - 1 ).split( "," );
        List<String> items = Lists.newArrayList( split );
        String str = "(";

        for ( int i = 0; i < items.size(); i++ )
        {
            Object item = QueryUtils.parseValue( items.get( i ) );
            if ( item != null )
            {
                str += item;
                if ( i < items.size() - 1 )
                {
                    str += ",";
                }
            }
        }

        str += ")";

        return str;
    }


    /**
     * Converts a filter operator into an SQL operator.
     * <p>
     * Example: {@code parseFilterOperator('eq', 5)}  will return "=5".
     *
     * @param operator the filter operator.
     * @param value value of the current SQL query condition.
     * @return a string contains an SQL expression with operator and value.
     */
    public static String parseFilterOperator( String operator, String value )
    {

        if ( StringUtils.isEmpty( operator ) )
        {
            throw new QueryParserException( "Filter Operator is null" );
        }

        switch ( operator )
        {
        case "eq":
        {
            return "= " + QueryUtils.parseValue( value );
        }
        case "!eq":
        {
            return "!= " + QueryUtils.parseValue( value );
        }
        case "ne":
        {
            return "!= " + QueryUtils.parseValue( value );
        }
        case "neq":
        {
            return "!= " + QueryUtils.parseValue( value );
        }
        case "gt":
        {
            return "> " + QueryUtils.parseValue( value );
        }
        case "lt":
        {
            return "< " + QueryUtils.parseValue( value );
        }
        case "gte":
        {
            return ">= " + QueryUtils.parseValue( value );
        }
        case "ge":
        {
            return ">= " + QueryUtils.parseValue( value );
        }
        case "lte":
        {
            return "<= " + QueryUtils.parseValue( value );
        }
        case "le":
        {
            return "<= " + QueryUtils.parseValue( value );
        }
        case "like":
        {
            return "like '%" + value + "%'";
        }
        case "!like":
        {
            return "not like '%" + value + "%'";
        }
        case "^like":
        {
            return " like '" + value + "%'";
        }
        case "!^like":
        {
            return " not like '" + value + "%'";
        }
        case "$like":
        {
            return " like '%" + value + "'";
        }
        case "!$like":
        {
            return " not like '%" + value + "'";
        }
        case "ilike":
        {
            return " ilike '%" + value + "%'";
        }
        case "!ilike":
        {
            return " not ilike '%" + value + "%'";
        }
        case "^ilike":
        {
            return " ilike '" + value + "%'";
        }
        case "!^ilike":
        {
            return " not ilike '" + value + "%'";
        }
        case "$ilike":
        {
            return " ilike '%" + value + "'";
        }
        case "!$ilike":
        {
            return " not ilike '%" + value + "'";
        }
        case "in":
        {
            return "in " + QueryUtils.convertCollectionValue( value );
        }
        case "!in":
        {
            return " not in " + QueryUtils.convertCollectionValue( value );
        }
        case "null":
        {
            return "is null";
        }
        case "!null":
        {
            return "is not null";
        }
        default:
        {
            throw new QueryParserException( "`" + operator + "` is not a valid operator." );
        }
        }
    }

    /**
     * Converts the specified string orders (e.g. <code>name:asc</code>) to order objects.
     *
     * @param orders the order strings that should be converted.
     * @param schema the schema that should be used to perform the conversion.
     * @return the converted order.
     */
    @Nonnull
    public static List<Order> convertOrderStrings( @Nullable Collection<String> orders, @Nonnull Schema schema )
    {
        if ( orders == null )
        {
            return Collections.emptyList();
        }

        final Map<String, Order> result = new LinkedHashMap<>();
        for ( String o : orders )
        {
            String[] split = o.split( ":" );

            String direction = "asc";

            if ( split.length < 1 )
            {
                continue;
            }
            else if ( split.length == 2 )
            {
                direction = split[1].toLowerCase();
            }

            String propertyName = split[0];
            Property property = schema.getProperty( propertyName );


            if ( result.containsKey( propertyName ) || !schema.haveProperty( propertyName )
                || !validProperty( property ) || !validDirection( direction ) )
            {
                continue;
            }

            result.put( propertyName, Order.from( direction, property ) );
        }

        return new ArrayList<>( result.values() );
    }

    private static boolean validProperty( Property property )
    {
        return property.isSimple();
    }

    private static boolean validDirection( String direction )
    {
        return "asc".equals( direction ) || "desc".equals( direction )
            || "iasc".equals( direction ) || "idesc".equals( direction );
    }

    /**
     * Returns a single result from the given {@link TypedQuery}. Returns null
     * if no objects could be found (without throwing an exception).
     *
     * @param query the query.
     * @return an object.
     */
    public static <T> T getSingleResult( TypedQuery<T> query )
    {
        query.setMaxResults( 1 );

        List<T> list = query.getResultList();

        if ( list == null || list.isEmpty() )
        {
            return null;
        }

        return list.get( 0 );
    }
}
