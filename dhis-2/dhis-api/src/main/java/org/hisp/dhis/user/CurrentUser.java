package org.hisp.dhis.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used to annotate controller parameters of type {@link User},
 * {@link UserCredentials} and {@link UserInfo} if the current user as returned
 * by the {@link CurrentUserService} should be injected.
 *
 * If the annotation is used with a {@link String} parameter the
 * {@link CurrentUserService#getCurrentUsername()} is injected.
 *
 * @author Jan Bernitt
 */
@Target( ElementType.PARAMETER )
@Retention( RetentionPolicy.RUNTIME )
public @interface CurrentUser
{
    /**
     * @return When true, the current user must be present otherwise an
     *         exception is thrown when resolving the parameter.
     */
    boolean required() default false;

    /**
     * @return When true, a web message wrapper exception is thrown when current
     *         user is {@link #required()} but not present
     */
    boolean wrap() default false;
}
