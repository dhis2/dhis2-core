package org.hisp.dhis.query.member;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.function.Predicate;

import org.hisp.dhis.query.member.MemberQuery.Field;
import org.hisp.dhis.query.member.MemberQuery.Filter;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;

final class MemberQueryLogic
{

    static List<Field> getDefaultFields( Schema schema )
    {
        Predicate<Property> filter = schema.isEmbeddedObject()
            ? MemberQueryLogic::isDefaultEmbeddedField
            : MemberQueryLogic::isDefaultField;
        return schema.getProperties().stream()
            .filter( filter )
            .map( p -> new Field( p.key(), p.getRelationViewDisplayAs() ) )
            .collect( toList() );
    }

    static boolean isDefaultField( Property p )
    {
        return p.isPersisted()
            && (!p.isCollection() || p.isEmbeddedObject() && !isRelationField( p ))
            && p.isReadable()
            && p.getFieldName() != null
            && (p.isSimple() || p.isEmbeddedObject())
            && (!isRelationField( p ) || p.isRequired());
    }

    /**
     * @return true for a {@link Property} that should be included by default in
     *         case it is a direct member of an embedded collection item object
     */
    static boolean isDefaultEmbeddedField( Property p )
    {
        return isDefaultField( p )
            || (p.isPersisted() && p.isRequired() && (p.isManyToOne() || p.isOneToOne()));
    }

    /**
     * @return true for a {@link Property} that should be included if it occurs
     *         in a nested (e.g. embedded) object
     */
    static boolean isNestedField( Property p )
    {
        return isDefaultField( p ) && !isRelationField( p );
    }

    static boolean isRelationField( Property p )
    {
        return p.isPersisted() && (p.isManyToMany() || p.isManyToOne() || p.isOneToOne() || p.getCascade() != null
            || p.getInverseRole() != null);
    }

    static Class<?> getSimpleField( Property p )
    {
        return p.isCollection() ? p.getItemKlass() : p.getKlass();
    }

    static boolean isLocalProperty( String path )
    {
        return path.indexOf( '.' ) < 0;
    }

    static boolean isIdProperty( Property p )
    {
        return "id".equals( p.key() ) && p.getKlass() == String.class;
    }

    static boolean isCollectionSizeFilter( Filter filter, Property property )
    {
        return isLocalProperty( filter.getPropertyPath() )
            && filter.getOperator().isOrderCompare()
            && property.isCollection();
    }

    private MemberQueryLogic()
    {
        throw new UnsupportedOperationException( "utility" );
    }
}
