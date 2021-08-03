package org.hisp.dhis.webapi.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark members in {@link JsonObject} that are expected to exist.
 *
 * This can only be applied to methods without parameters.
 *
 * @author Jan Bernitt
 */
@Target( ElementType.METHOD )
@Retention( RetentionPolicy.RUNTIME )
public @interface Expected
{
    /**
     * @return Can be set to {@code true} to allow {@link JsonValue}s either
     *         being set or being a JSON {@code null} value. Default value is
     *         {@code false}.
     */
    boolean nullable() default false;
}
