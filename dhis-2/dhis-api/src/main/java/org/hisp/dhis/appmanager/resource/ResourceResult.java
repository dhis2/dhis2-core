package org.hisp.dhis.appmanager.resource;

/**
 * Models the potential results when trying to retrieve a Resource. <br>
 * Can be one of:
 *
 * <ul>
 *   <li>ResourceFound
 *   <li>ResourceNotFound
 *   <li>Redirect
 * </ul>
 *
 * <p>This enables:
 *
 * <ul>
 *   <li>clearer understanding of control flow & intent
 *   <li>easier handling of multiple scenarios without having to deal with nulls or exceptions
 *   <li>easier extension potential
 * </ul>
 */
public sealed interface ResourceResult permits ResourceFound, ResourceNotFound, Redirect {}
