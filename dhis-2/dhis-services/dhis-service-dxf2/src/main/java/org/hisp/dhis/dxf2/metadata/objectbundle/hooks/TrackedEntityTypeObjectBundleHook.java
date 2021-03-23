package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2019, University of Oslo
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
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component("org.hisp.dhis.dxf2.metadata.objectbundle.hooks.TrackedEntityTypeObjectBundleHook")
public class TrackedEntityTypeObjectBundleHook extends AbstractObjectBundleHook {
	@Override
	public <T extends IdentifiableObject> List<ErrorReport> validate(T object, ObjectBundle bundle) {
		List<ErrorReport> errorReports = new ArrayList<>();

		Optional.ofNullable(object).filter(o -> o.getClass().isAssignableFrom(TrackedEntityType.class))
				.map(o -> (TrackedEntityType) o)
				.flatMap(trackedEntityType -> Optional.ofNullable(trackedEntityType.getTrackedEntityAttributes()))
				.ifPresent(teAttrs -> teAttrs.stream().filter(Objects::nonNull).forEach(tea -> {

					if (bundle.getPreheat().get(bundle.getPreheatIdentifier(), tea) == null) {
						errorReports.add(

								new ErrorReport(TrackedEntityType.class, ErrorCode.E5001, bundle.getPreheatIdentifier(),
										bundle.getPreheatIdentifier().getIdentifiersWithName(tea))
												.setErrorProperty("id").setMainId(tea.getUid()));
					}

				}));

		return errorReports;
	}
}
