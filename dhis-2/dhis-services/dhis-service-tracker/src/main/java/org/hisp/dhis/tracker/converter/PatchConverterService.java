package org.hisp.dhis.tracker.converter;

import org.hisp.dhis.tracker.preheat.TrackerPreheat;

/**
 * @author Luciano Fiandesio
 */
public interface PatchConverterService<From, To> {

    To fromForPatch(TrackerPreheat preheat, From object );
}
