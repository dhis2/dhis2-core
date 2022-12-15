/**
 * Package validation takes a potentially invalid user payload
 * ({@link org.hisp.dhis.tracker.bundle.TrackerBundle} in and runs all our
 * {@link org.hisp.dhis.tracker.validation.Validator}s attributing errors and
 * warnings to entities in the payload. The
 * {@link org.hisp.dhis.tracker.bundle.TrackerBundle} leaving this package can
 * be persisted (created, updated, deleted) and thus trusted to be in a valid
 * state. {@link org.hisp.dhis.tracker.validation.TrackerValidationService} is
 * the main entry point into this package. The result of the validation would be
 * a {@link org.hisp.dhis.tracker.validation.ValidationResult} that contains a
 * list of errors and a list of warnings both of them exposed as
 * {@link org.hisp.dhis.tracker.validation.Validation}s.
 */
package org.hisp.dhis.tracker.validation;