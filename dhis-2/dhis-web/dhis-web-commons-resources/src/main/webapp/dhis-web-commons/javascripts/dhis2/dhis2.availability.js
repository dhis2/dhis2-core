"use strict";

/*
 * Copyright (c) 2004-2014, University of Oslo
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

dhis2.util.namespace('dhis2.availability');

dhis2.availability._isAvailable = -1;
dhis2.availability._isLoggedIn = -1;
dhis2.availability._availableTimeoutHandler = -1;

/**
 * Start availability check, will trigger dhis2.online / dhis2.offline events
 * when availability changes.
 *
 * @param onlineInterval How often to check for availability when online,
 *            default is 10000.
 * @param offlineInterval How often to check for availability when offline,
 *            default is 1000.
 */
dhis2.availability.startAvailabilityCheck = function( onlineInterval, offlineInterval ) {
  onlineInterval = onlineInterval ? onlineInterval : 30000;
  offlineInterval = offlineInterval ? offlineInterval : 1000;

  function _checkAvailability() {
    $.ajax({
      url: "../dhis-web-commons-stream/ping.action",
      cache: false,
      timeout: 30000,
      dataType: "text",
      success: function( data, textStatus, jqXHR ) {
        try {
          data = JSON.parse(data);
        } catch( e ) {
        }

        dhis2.availability._isAvailable = true;
        var loggedIn = data.loggedIn ? true : false;

        if( loggedIn != dhis2.availability._isLoggedIn ) {
          dhis2.availability._isLoggedIn = loggedIn;
          $(document).trigger("dhis2.online", [ loggedIn ]);
        }
      },
      error: function( jqXHR, textStatus, errorThrown ) {
        if( dhis2.availability._isAvailable ) {
          dhis2.availability._isAvailable = false;
          dhis2.availability._isLoggedIn = -1;
          $(document).trigger("dhis2.offline");
        }
      },
      complete: function() {
        if( dhis2.availability._isAvailable ) {
          dhis2.availability._availableTimeoutHandler = setTimeout(_checkAvailability, onlineInterval);
        }
        else {
          dhis2.availability._availableTimeoutHandler = setTimeout(_checkAvailability, offlineInterval);
        }
      }
    });
  }

  // use 500ms for initial check
  setTimeout(_checkAvailability, 500);
};

/**
 * Stop checking for availability.
 */
dhis2.availability.stopAvailabilityCheck = function() {
  clearTimeout(dhis2.availability._availableTimeoutHandler);
};

/**
 * Synchronized one-off check of availability.
 */
dhis2.availability.syncCheckAvailability = function() {
  var isLoggedIn = false;

  $.ajax({
    url: "../dhis-web-commons-stream/ping.action",
    async: false,
    cache: false,
    timeout: 30000,
    dataType: "json",
    success: function( data, textStatus, jqXHR ) {
      dhis2.availability._isAvailable = true;
      var loggedIn = data.loggedIn ? true : false;

      if( loggedIn != dhis2.availability._isLoggedIn ) {
        dhis2.availability._isLoggedIn = loggedIn;
        $(document).trigger("dhis2.online", [ loggedIn ]);
      }

      isLoggedIn = loggedIn;
    },
    error: function( jqXHR, textStatus, errorThrown ) {
      if( dhis2.availability._isAvailable ) {
        dhis2.availability._isAvailable = false;
        dhis2.availability._isLoggedIn = -1;
        $(document).trigger("dhis2.offline");
      }
    }
  });

  return isLoggedIn;
};
