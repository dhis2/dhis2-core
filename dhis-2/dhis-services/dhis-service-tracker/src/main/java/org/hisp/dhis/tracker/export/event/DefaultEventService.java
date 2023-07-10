/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.common.Pager.DEFAULT_PAGE_SIZE;
import static org.hisp.dhis.common.SlimPager.FIRST_PAGE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service("org.hisp.dhis.tracker.export.event.EventService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefaultEventService implements EventService {
  private final CurrentUserService currentUserService;

  private final EventStore eventStore;

  private final org.hisp.dhis.program.EventService eventService;

  private final TrackerAccessManager trackerAccessManager;

  private final DataElementService dataElementService;

  private final EventOperationParamsMapper paramsMapper;

  @Override
  public Event getEvent(String uid, EventParams eventParams)
      throws NotFoundException, ForbiddenException {
    Event event = eventService.getEvent(uid);
    if (event == null) {
      throw new NotFoundException(Event.class, uid);
    }

    return getEvent(event, eventParams);
  }

  @Override
  public Event getEvent(@Nonnull Event event, EventParams eventParams) throws ForbiddenException {
    List<String> errors =
        trackerAccessManager.canRead(currentUserService.getCurrentUser(), event, false);
    if (!errors.isEmpty()) {
      throw new ForbiddenException(errors.toString());
    }

    Event result = new Event();
    result.setUid(event.getUid());

    result.setStatus(event.getStatus());
    result.setExecutionDate(event.getExecutionDate());
    result.setDueDate(event.getDueDate());
    result.setStoredBy(event.getStoredBy());
    result.setCompletedBy(event.getCompletedBy());
    result.setCompletedDate(event.getCompletedDate());
    result.setCreated(event.getCreated());
    result.setCreatedByUserInfo(event.getCreatedByUserInfo());
    result.setLastUpdatedByUserInfo(event.getLastUpdatedByUserInfo());
    result.setCreatedAtClient(event.getCreatedAtClient());
    result.setLastUpdated(event.getLastUpdated());
    result.setLastUpdatedAtClient(event.getLastUpdatedAtClient());
    result.setGeometry(event.getGeometry());
    result.setDeleted(event.isDeleted());
    result.setAssignedUser(event.getAssignedUser());

    OrganisationUnit ou = event.getOrganisationUnit();

    result.setEnrollment(event.getEnrollment());
    result.setProgramStage(event.getProgramStage());

    result.setOrganisationUnit(ou);
    result.setProgramStage(event.getProgramStage());

    result.setAttributeOptionCombo(event.getAttributeOptionCombo());

    for (EventDataValue dataValue : event.getEventDataValues()) {
      if (dataElementService.getDataElement(dataValue.getDataElement())
          != null) // check permissions
      {
        EventDataValue value = new EventDataValue();
        value.setCreated(dataValue.getCreated());
        value.setCreatedByUserInfo(dataValue.getCreatedByUserInfo());
        value.setLastUpdated(dataValue.getLastUpdated());
        value.setLastUpdatedByUserInfo(dataValue.getLastUpdatedByUserInfo());
        value.setDataElement(dataValue.getDataElement());
        value.setValue(dataValue.getValue());
        value.setProvidedElsewhere(dataValue.getProvidedElsewhere());
        value.setStoredBy(dataValue.getStoredBy());

        result.getEventDataValues().add(value);
      } else {
        log.info("Can not find a Data Element having UID [" + dataValue.getDataElement() + "]");
      }
    }

    result.getComments().addAll(event.getComments());

    User user = currentUserService.getCurrentUser();
    if (eventParams.isIncludeRelationships()) {
      Set<RelationshipItem> relationshipItems = new HashSet<>();

      for (RelationshipItem relationshipItem : event.getRelationshipItems()) {
        org.hisp.dhis.relationship.Relationship daoRelationship =
            relationshipItem.getRelationship();
        if (trackerAccessManager.canRead(user, daoRelationship).isEmpty()
            && (!daoRelationship.isDeleted())) {
          relationshipItems.add(relationshipItem);
        }
      }

      result.setRelationshipItems(relationshipItems);
    }

    return result;
  }

  @Override
  public Events getEvents(EventOperationParams operationParams)
      throws BadRequestException, ForbiddenException {
    EventSearchParams searchParams = paramsMapper.map(operationParams);

    if (!operationParams.isPaging() && !operationParams.isSkipPaging()) {
      operationParams.setDefaultPaging();
    }

    Events events = new Events();

    if (operationParams.isSkipPaging()) {
      events.setEvents(eventStore.getEvents(searchParams, emptyMap()));
      return events;
    }

    Pager pager;
    List<Event> eventList = new ArrayList<>(eventStore.getEvents(searchParams, emptyMap()));

    if (operationParams.isTotalPages()) {
      int count = eventStore.getEventCount(searchParams);
      pager =
          new Pager(
              operationParams.getPageWithDefault(),
              count,
              operationParams.getPageSizeWithDefault());
    } else {
      pager = handleLastPageFlag(operationParams, eventList);
    }

    events.setPager(pager);
    events.setEvents(eventList);

    return events;
  }

  /**
   * This method will apply the logic related to the parameter 'totalPages=false'. This works in
   * conjunction with the method: {@link
   * EventStore#getEvents(EventSearchParams,Map<String,Set<String>>)}
   *
   * <p>This is needed because we need to query (pageSize + 1) at DB level. The resulting query will
   * allow us to evaluate if we are in the last page or not. And this is what his method does,
   * returning the respective Pager object.
   *
   * @param params the request params
   * @param eventList the reference to the list of Event
   * @return the populated SlimPager instance
   */
  private Pager handleLastPageFlag(EventOperationParams params, List<Event> eventList) {
    Integer originalPage = defaultIfNull(params.getPage(), FIRST_PAGE);
    Integer originalPageSize = defaultIfNull(params.getPageSize(), DEFAULT_PAGE_SIZE);
    boolean isLastPage = false;

    if (isNotEmpty(eventList)) {
      isLastPage = eventList.size() <= originalPageSize;
      if (!isLastPage) {
        // Get the same number of elements of the pageSize, forcing
        // the removal of the last additional element added at querying
        // time.
        eventList.retainAll(eventList.subList(0, originalPageSize));
      }
    }

    return new SlimPager(originalPage, originalPageSize, isLastPage);
  }
}
