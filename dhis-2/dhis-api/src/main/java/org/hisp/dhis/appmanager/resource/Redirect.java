package org.hisp.dhis.appmanager.resource;

import javax.annotation.Nonnull;

/**
 * Should be used when a HTTP redirect is required
 *
 * @param path the redirect path to use
 */
public record Redirect(@Nonnull String path) implements ResourceResult {}
