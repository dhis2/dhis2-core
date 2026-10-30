/**
 * SQL support components for {@code ENROLLMENT_OU} handling in event analytics.
 *
 * <p>This package isolates:
 *
 * <ul>
 *   <li>shared SQL constants and aliases for enrollment org unit columns,
 *   <li>reusable SQL fragment builders for select/join/where clauses,
 *   <li>coordination logic that applies those fragments across query and aggregate flows, and
 *   <li>row-access helpers for reading {@code ENROLLMENT_OU} values from result sets.
 * </ul>
 */
package org.hisp.dhis.analytics.event.data.ou;
