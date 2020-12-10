package org.hisp.dhis.tracker.domain.verbs;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author Luciano Fiandesio
 */
@Retention( RUNTIME )
@Target( { ElementType.FIELD } )
public @interface ValidFor
{
    RestVerbs[] verb() default RestVerbs.ALL;
}
