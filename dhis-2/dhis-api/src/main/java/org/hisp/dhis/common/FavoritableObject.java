package org.hisp.dhis.common;

import java.util.Set;
import org.hisp.dhis.user.UserDetails;

/**
 * Interface for objects which can be marked as favorite by users.
 * Object implementing this interface must have a property of type {@code Set<String>}
 * with the name 'favorites' where the set contains the UIDs of users having
 * marked the object as favorite.
 */
public interface FavoritableObject {
  
  Set<String> getFavorites();

  boolean isFavorite();

  boolean setAsFavorite(UserDetails user);

  boolean removeAsFavorite(UserDetails user);

}
