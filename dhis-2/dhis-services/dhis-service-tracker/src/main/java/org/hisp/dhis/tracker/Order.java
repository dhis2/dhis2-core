/*
 * Copyright (c) 2004-2023, University of Oslo
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
 */
package org.hisp.dhis.tracker;

import lombok.Value;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;

/**
 * Tracker exporter APIs allow ordering by different types. For example events can be ordered by
 * field names, {@link DataElement} and {@link TrackedEntityAttribute}. It is crucial for the order
 * values to stay in one collection as their order needs to be kept as provided by the user. We
 * cannot come up with a type-safe type that captures the above order features and that can be used
 * in a generic collection such as a List (see typesafe heterogeneous container). We therefore use
 * {@link Order} with a field of type {@link Object}. We get compile time type safety via methods
 * such as {@link org.hisp.dhis.tracker.export.event.EventSearchParams#orderBy(DataElement,
 * SortDirection)}. This allows us to advocate the types that can be ordered by while storing the
 * order in a single {@code List} of {@link Order}. Runtime type checks are then used to ensure
 * users (and developers) get meaningful error messages in case they order by unsupported fields or
 * types.
 */
@Value
public class Order {
  Object field;
  SortDirection direction;
}
