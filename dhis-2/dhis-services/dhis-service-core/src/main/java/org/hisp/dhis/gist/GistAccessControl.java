package org.hisp.dhis.gist;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * Access for the {@link #getCurrentUser()} within the currently processed gist
 * API request.
 *
 * This encapsulates all access related logic of gist API request processing.
 */
public interface GistAccessControl
{

    /**
     * It can be assumed that the returned value is "cached" and cheap to be
     * accessed.
     *
     * @return The current user of the currently processed gist API request this
     *         {@link GistAccessControl} belongs to
     */
    User getCurrentUser();

    /**
     * Whether or not the {@link #getCurrentUser()} is a superuser.
     *
     * @return true, if the current user is a superuser, otherwise false
     */
    boolean isSuperuser();

    /**
     * Whether or not the {@link #getCurrentUser()} can read the field
     * {@link Property} belonging to objects of the {@link Schema} type.
     *
     * This will be called to determine which fields to include in "all" fields
     * and similar presets and also to check that only accessible fields are
     * shown to the {@link #getCurrentUser()}.
     *
     * @param type of the owner object, e.g. a
     *        {@link org.hisp.dhis.organisationunit.OrganisationUnit}
     * @param field {@link Property} of the owner type to check
     * @return true, if the current user can generally read the field value for
     *         objects of the provided type (sharing may still disallow this for
     *         individual values which are filtered by added sharing based
     *         filters to gist queries)
     */
    boolean canRead( Class<? extends IdentifiableObject> type, Property field );

    /**
     * The {@link Access} capabilities of the {@link #getCurrentUser()} given
     * the {@link Sharing} and type of object the sharing belongs to.
     *
     * This is called for every result row to convert the {@link Sharing}
     * information as stored in the database into the {@link Access} if that
     * field were requested explicitly.
     *
     * @param type of the owner object, e.g. a
     *        {@link org.hisp.dhis.organisationunit.OrganisationUnit}
     * @param value actual {@link Sharing} value of the {@code sharing} property
     *        of an object of the provided type to use to compute the
     *        {@link Access} for the current user
     * @return {@link Access} for the current user given the provided
     *         {@link Sharing} value
     */
    Access canAccess( Class<? extends IdentifiableObject> type, Sharing value );

}
