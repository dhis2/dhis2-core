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
package org.hisp.dhis.tracker.program.message;

import java.util.Set;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public abstract class DeliveryChannelStrategy {
  // -------------------------------------------------------------------------
  // Abstract methods
  // -------------------------------------------------------------------------

  protected abstract DeliveryChannel getDeliveryChannel();

  protected abstract ProgramMessage setAttributes(ProgramMessage message);

  protected abstract void validate(ProgramMessage message);

  // -------------------------------------------------------------------------
  // Public methods
  // -------------------------------------------------------------------------

  public String getTrackedEntityRecipient(TrackedEntity te, ValueType type) {
    Set<TrackedEntityAttributeValue> attributeValues = te.getTrackedEntityAttributeValues();

    for (TrackedEntityAttributeValue value : attributeValues) {
      if (value != null
          && value.getAttribute().getValueType().equals(type)
          && value.getValue() != null
          && !value.getValue().trim().isEmpty()) {
        return value.getValue();
      }
    }

    throw new IllegalQueryException(
        "Tracked entity does not have any attribute of value type: " + type.toString());
  }
}
