/**
 * Contains the view (JSON) models of tracker. Imports and exports share the
 * same model. Import mappers in
 * {@link org.hisp.dhis.webapi.controller.tracker.imports} translate JSON
 * payloads (view models) to the {@link org.hisp.dhis.tracker.domain} which is
 * used internally. Export mappers in
 * {@link org.hisp.dhis.webapi.controller.tracker.export} translate from
 * {@link org.hisp.dhis} to the JSON payloads (view models) tracker exposes to
 * its users.
 */
package org.hisp.dhis.webapi.controller.tracker.view;