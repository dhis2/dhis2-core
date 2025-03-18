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
package org.hisp.dhis.tracker.imports.validation;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;

/**
 * @author Luciano Fiandesio
 */
class MessageFormatter {

  private MessageFormatter() {
    // not meant to be inherited from
  }

  /**
   * Creates an interpolated string using given {@code messagePattern} ({@link MessageFormat}) and
   * {@code arguments}. {@code arguments} are pre-processed to be rendered in a type specific
   * manner. For example {@link Instant} are displayed in ISO 8601, without any TZ info. Identifiers
   * for {@link IdentifiableObject} are displayed in the user chosen idScheme ({@code idSchemes}).
   *
   * @param idSchemes idSchemes to use when rendering identifiers
   * @param messagePattern message format pattern
   * @param arguments arguments representing format elements in the message pattern
   * @return interpolated string of given message pattern and arguments
   */
  protected static String format(
      TrackerIdSchemeParams idSchemes, String messagePattern, Object... arguments) {
    List<String> args = formatArguments(idSchemes, arguments);
    return MessageFormat.format(messagePattern, args.toArray(new Object[0]));
  }

  protected static List<String> formatArguments(
      TrackerIdSchemeParams idSchemes, Object... arguments) {
    List<String> args = new ArrayList<>();
    for (Object arg : arguments) {
      args.add(formatArgument(idSchemes, arg));
    }
    return args;
  }

  private static String formatArgument(TrackerIdSchemeParams idSchemes, Object argument) {
    if (argument == null) return "NULL";
    Class<?> type = argument.getClass();
    if (String.class == type) return argument.toString();
    if (MetadataIdentifier.class.isAssignableFrom(type))
      return ((MetadataIdentifier) argument).getIdentifierOrAttributeValue();
    if (CategoryOptionCombo.class.isAssignableFrom(type))
      return getIdAndName(idSchemes.toMetadataIdentifier((CategoryOptionCombo) argument));
    if (CategoryOption.class.isAssignableFrom(type))
      return getIdAndName(idSchemes.toMetadataIdentifier((CategoryOption) argument));
    if (DataElement.class.isAssignableFrom(type))
      return getIdAndName(idSchemes.toMetadataIdentifier((DataElement) argument));
    if (OrganisationUnit.class.isAssignableFrom(type))
      return getIdAndName(idSchemes.toMetadataIdentifier((OrganisationUnit) argument));
    if (Program.class.isAssignableFrom(type))
      return getIdAndName(idSchemes.toMetadataIdentifier((Program) argument));
    if (ProgramStage.class.isAssignableFrom(type))
      return getIdAndName(idSchemes.toMetadataIdentifier((ProgramStage) argument));
    if (IdentifiableObject.class.isAssignableFrom(type))
      return getIdAndName(idSchemes.toMetadataIdentifier((IdentifiableObject) argument));
    if (UserDetails.class.isAssignableFrom(type)) return ((UserDetails) argument).getUid();
    if (Date.class.isAssignableFrom(type)) return (DateFormat.getInstance().format(argument));
    if (Instant.class.isAssignableFrom(type))
      return DateUtils.toIso8601NoTz(DateUtils.fromInstant((Instant) argument));
    if (Enrollment.class.isAssignableFrom(type))
      return ((Enrollment) argument).getEnrollment().getValue();
    if (Event.class.isAssignableFrom(type)) return ((Event) argument).getEvent().getValue();
    if (TrackedEntity.class.isAssignableFrom(type))
      return ((TrackedEntity) argument).getTrackedEntity().getValue();
    return argument.toString();
  }

  private static String getIdAndName(MetadataIdentifier identifier) {
    return identifier.getIdentifierOrAttributeValue();
  }
}
