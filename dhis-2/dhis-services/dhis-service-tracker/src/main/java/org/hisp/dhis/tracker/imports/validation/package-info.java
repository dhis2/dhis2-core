/**
 * Package validation takes a potentially invalid user payload ({@link
 * org.hisp.dhis.tracker.imports.bundle.TrackerBundle} in and runs all our {@link
 * org.hisp.dhis.tracker.imports.validation.Validator}s attributing errors and warnings to entities
 * {@link org.hisp.dhis.tracker.imports.domain.TrackerDto} in the payload. {@link
 * org.hisp.dhis.tracker.imports.validation.ValidationService} is the main entry point into this
 * package. The result of the validation is a {@link
 * org.hisp.dhis.tracker.imports.validation.ValidationResult} that contains a list of errors and a
 * list of warnings both of them exposed as {@link
 * org.hisp.dhis.tracker.imports.validation.Validation}s. The {@link
 * org.hisp.dhis.tracker.imports.validation.ValidationResult}s tracked entities, enrollments, events
 * and relationships can be persisted (created, updated, deleted) and thus trusted to be in a valid
 * state.
 */
package org.hisp.dhis.tracker.imports.validation;
