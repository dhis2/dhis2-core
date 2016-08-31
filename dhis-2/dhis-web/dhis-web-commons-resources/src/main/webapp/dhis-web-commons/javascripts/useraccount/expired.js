/*
 * Copyright (c) 2004-2013, University of Oslo
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

var validationRules = {
  rules: {
    oldPassword: {
      required: true
    },
    password: {
      required: true,
      rangelength: [8, 80],
      password: true,
      notequalto: "#oldPassword"
    },
    retypePassword: {
      required: true,
      equalTo: "#password"
    }
  }
};

$(document).ready(function() {
  $("#accountForm").validate({
    rules: validationRules.rules,
    submitHandler: accountSubmitHandler,
    errorPlacement: function(error, element) {
      element.parent("td").append("<br>").append(error);
    }
  });
});

function accountSubmitHandler() {
  $("#submitButton").attr("disabled", "disabled");

  $.ajax({
    url: '../../api/account/password',
    data: $("#accountForm").serialize(),
    type: 'POST',
    success: function(data) {
      window.location.href = "../../dhis-web-commons-about/redirect.action";
    },
    error: function(jqXHR, textStatus, errorThrown) {
      var data = JSON.parse(jqXHR.responseText);

      if( data.status === 'NON_EXPIRED' ) {
        window.location.href = "login.action";
      }

      $("#messageSpan").show().text(data.message);
      $("#submitButton").removeAttr("disabled");
    }
  });
}
