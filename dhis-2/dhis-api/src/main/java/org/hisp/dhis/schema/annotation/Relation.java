package org.hisp.dhis.schema.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelationViewType;

/**
 * Apply {@link Property#getRelationViewDisplayAs()} and
 * {@link Property#getRelationViewDisplayOptions()} to a getter or type.
 *
 * Type level annotations apply to all getters. Annotations on getter overwrite
 * those on type.
 *
 * @author Jan Bernitt
 */
@Documented
@Inherited
@Target( { ElementType.METHOD, ElementType.TYPE } )
@Retention( RetentionPolicy.RUNTIME )
public @interface Relation
{
    /**
     * @return The list of fields shown when the referenced object is included
     *         in a view. The empty list applies automatic selection based on
     *         schema.
     */
    String[] fields() default {};

    /**
     * @return the type used in case the user has not specified the type
     *         explicitly.
     */
    RelationViewType displayAs() default RelationViewType.AUTO;

    /**
     * @return The set of types that can be used (are permitted). If a type is
     *         not included in the set but requested by a request the request is
     *         either denied or a permitted type is chosen instead.
     */
    RelationViewType[] displayOptions() default {
        RelationViewType.REF,
        RelationViewType.COUNT,
        RelationViewType.IDS,
        RelationViewType.ID_OBJECTS };
}
