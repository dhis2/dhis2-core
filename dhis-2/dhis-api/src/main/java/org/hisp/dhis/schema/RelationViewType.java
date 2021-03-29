package org.hisp.dhis.schema;

/**
 * The {@link RelationViewType} controls how relations between
 * {@link org.hisp.dhis.common.IdentifiableObject}s are displayed.
 *
 * The intention is to provide guidance and protection so that request provide
 * useful information units while avoiding overly large responses that are slow
 * and resource intensive.
 *
 * @author Jan Bernitt
 */
public enum RelationViewType
{
    /**
     * No actual type but used to represent the choice that the actual value
     * should be determined by program logic considering other relevant
     * metadata.
     */
    AUTO,

    /**
     * The relation is not shown as data but as a endpoint description that
     * allows to browse the data.
     */
    REF,

    /**
     * The relation is not shown as data but as endpoint description that allows
     * to browse the data. In addition this description contains the number of
     * referenced elements. This number does not consider access limitations but
     * represents the raw data as contained in the database.
     */
    COUNT,

    /**
     * The relation is shown as a list of UIDs. In addition an endpoint
     * description is given that allows to browse the full data objects
     * represented by the ids.
     */
    IDS,

    /**
     * Identical to {@link #IDS} except that each entry is still represented as
     * object with a single property {@code id}. This is mostly for easier
     * transition from existing APIs that usually return this type of ID lists.
     *
     * <pre>
     * { "id": UID }
     * </pre>
     *
     * (instead of plain UID)
     */
    ID_OBJECTS
}
