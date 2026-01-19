/**
 * This package contains DHIS2 tracker data related code.
 *
 * <p>The dhis-tracker module provides service layer implementations for all tracker data
 * operations, including importing, exporting, and managing tracker entities such as tracked
 * entities, enrollments, events, and relationships.
 *
 * <h2>Scope</h2>
 *
 * <p>This module includes:
 *
 * <ul>
 *   <li>Tracker data import services
 *   <li>Tracker data export services
 *   <li>Access control and ownership management
 *   <li>Audit and change log functionality
 *   <li>Notification and message functionality
 *   <li>Deduplication services for tracked entities
 * </ul>
 *
 * <h2>Important Notes</h2>
 *
 * <ul>
 *   <li><strong>Metadata Exclusion:</strong> This module does NOT contain tracker metadata code
 *       (e.g., Program, ProgramStage, TrackedEntityType definitions). Metadata entities are defined
 *       and managed in other modules.
 *   <li><strong>Web Layer Separation:</strong> All web/API code including controllers, REST
 *       endpoints, and request/response handling remains in the dhis-web-api module. This module
 *       focuses exclusively on the service and data access layers.
 * </ul>
 *
 * @since 2.43
 */
package org.hisp.dhis.tracker;
