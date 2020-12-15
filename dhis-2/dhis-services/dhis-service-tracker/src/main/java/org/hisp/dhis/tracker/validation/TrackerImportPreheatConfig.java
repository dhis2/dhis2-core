package org.hisp.dhis.tracker.validation;

/*
 * Copyright (c) 2004-2020, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import java.util.List;

import org.hisp.dhis.tracker.preheat.supplier.ClassBasedSupplier;
import org.hisp.dhis.tracker.preheat.supplier.ProgramInstancesWithAtLeastOneEventSupplier;
import org.hisp.dhis.tracker.preheat.supplier.FileResourceSupplier;
import org.hisp.dhis.tracker.preheat.supplier.PeriodTypeSupplier;
import org.hisp.dhis.tracker.preheat.supplier.PreheatSupplier;
import org.hisp.dhis.tracker.preheat.supplier.ProgramInstanceByTeiSupplier;
import org.hisp.dhis.tracker.preheat.supplier.ProgramInstanceSupplier;
import org.hisp.dhis.tracker.preheat.supplier.ProgramOrgUnitsSupplier;
import org.hisp.dhis.tracker.preheat.supplier.ProgramStageInstanceProgramStageMapSupplier;
import org.hisp.dhis.tracker.preheat.supplier.RelationshipTypeSupplier;
import org.hisp.dhis.tracker.preheat.supplier.TrackedEntityTypeSupplier;
import org.hisp.dhis.tracker.preheat.supplier.UniqueAttributesSupplier;
import org.hisp.dhis.tracker.preheat.supplier.UserSupplier;

import com.google.common.collect.ImmutableList;

import lombok.experimental.UtilityClass;

/**
 * Configuration class for the pre-heat stage. This class holds the list of
 * pre-heat suppliers executed during import
 */
@UtilityClass
public class TrackerImportPreheatConfig
{
    public static final List<Class<? extends PreheatSupplier>> PREHEAT_ORDER = ImmutableList.of(
        ClassBasedSupplier.class,
        ProgramInstanceSupplier.class,
        ProgramInstanceByTeiSupplier.class,
        ProgramInstancesWithAtLeastOneEventSupplier.class,
        ProgramStageInstanceProgramStageMapSupplier.class,
        ProgramOrgUnitsSupplier.class,
        TrackedEntityTypeSupplier.class,
        RelationshipTypeSupplier.class,
        PeriodTypeSupplier.class,
        UniqueAttributesSupplier.class,
        UserSupplier.class,
        FileResourceSupplier.class );
}
