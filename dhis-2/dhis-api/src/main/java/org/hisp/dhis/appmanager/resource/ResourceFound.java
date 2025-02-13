package org.hisp.dhis.appmanager.resource;

import javax.annotation.Nonnull;
import org.springframework.core.io.Resource;

/**
 * Should only be used with a Resource that exists
 *
 * @param resource the resource
 */
public record ResourceFound(@Nonnull Resource resource) implements ResourceResult {}
