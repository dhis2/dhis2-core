/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.common;

import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.OTHER;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;

import lombok.Getter;

/** Encapsulates some information about the current request and endpoint invoked. */
public class RequestTypeAware {

  @Getter private EndpointAction endpointAction = OTHER;

  @Getter private EndpointItem endpointItem;

  public RequestTypeAware withEndpointAction(EndpointAction endpointAction) {
    this.endpointAction = endpointAction;
    return this;
  }

  public RequestTypeAware withEndpointItem(EndpointItem endpointItem) {
    this.endpointItem = endpointItem;
    return this;
  }

  public boolean isQueryEndpoint() {
    return QUERY == endpointAction;
  }

  public boolean isAggregateEndpoint() {
    return AGGREGATE == endpointAction;
  }

  public boolean isEnrollmentEndpointItem() {
    return EndpointItem.ENROLLMENT == endpointItem;
  }

  public enum EndpointAction {
    AGGREGATE,
    QUERY,
    OTHER
  }

  public enum EndpointItem {
    EVENT,
    ENROLLMENT,
    TRACKED_ENTITY_INSTANCE
  }
}
