package org.hisp.dhis.appmanager.resource;

import javax.annotation.Nonnull;

/**
 * Should be used when a Resource does not exist.
 *
 * @param path the path at which the resource was attempted to be retrieved from
 */
public record ResourceNotFound(@Nonnull String path) implements ResourceResult {}
