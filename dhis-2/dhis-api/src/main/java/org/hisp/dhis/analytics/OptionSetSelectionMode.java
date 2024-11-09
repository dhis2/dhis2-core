package org.hisp.dhis.analytics;

/**
 * The selection modes for items with option sets
 */
public enum OptionSetSelectionMode {
    // All options in an option set are chosen and aggregated into a single column.
    // This selection is relative, so any new options added to the option set are included.
    AGGREGATED,
    // All options in an option set are chosen and displayed as data items.
    // This selection is relative, so any new options added to the option set are included.
    DISAGGREGATED,
    // choose options from an option set and display those as disaggregated data items.
    // This selection is non-relative and will not automatically include
    // any options added to the option set in the future.
    ABSOLUTE
}
