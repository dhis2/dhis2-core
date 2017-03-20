var validationRules = {
  rules: {
    firstName: {
      required: true,
      rangelength: [2, 80]
    },
    surname: {
      required: true,
      rangelength: [2, 80]
    },
    username: {
      required: true,
      rangelength: [4, 80],
      remote: "../../api/account/username"
    },
    password: {
      required: true,
      rangelength: [8, 40],
      password: true,
      remote: "../../api/account/password"

    },
    retypePassword: {
      required: true,
      equalTo: "#password",
    },
    email: {
      required: true,
      email: true,
      rangelength: [4, 80]
    },
    inviteEmail: {
      required: true,
      email: true,
      rangelength: [4, 80]
    },
    phoneNumber: {
      required: true,
      rangelength: [6, 30]
    },
    employer: {
      required: true,
      rangelength: [2, 80]
    }
  }
};


var login = {};
login.localeKey = "dhis2.locale.ui";

$(document).ready(function() {

  var locale = localStorage[login.localeKey];

  if( undefined !== locale && locale ) {
    login.changeLocale(locale);
  }

  if( recaptchaEnabled() ) {
    Recaptcha.create("6LcM6tcSAAAAANwYsFp--0SYtcnze_WdYn8XwMMk", "recaptchaDiv", {
      theme: "white"
    });
  }

  $("#accountForm").validate({
    rules: validationRules.rules,
    submitHandler: accountSubmitHandler,
    errorPlacement: function(error, element) {
      element.parent("td").append("<br>").append(error);
    }
  });
});

function checkPasswordForUsername() {

  var userName = $("#username").val();
  var passWord = $("#password").val();
  if (passWord) {
    if (userName) {
      if ((passWord.indexOf(userName) !== -1) ||  (userName.indexOf(passWord) !== -1)) {
        alert("Username cannot be part of password");
        $("#password").val("");      }
    }
  }

}

function checkPasswordForEmail() {
  var email = $("#email").val();
  var passWord = $("#password").val();
  if (passWord) {
    if (email) {
      if ((passWord.indexOf(email) !== -1) ||  (email.indexOf(passWord) !== -1)) {
        alert("Email cannot be part of password");
        $("#password").val("");
      }
    }
  }
}

function accountSubmitHandler() {
  if( recaptchaEnabled() ) {
    if( $.trim($("#recaptcha_challenge_field").val()).length == 0 ||
      $.trim($("#recaptcha_response_field").val()).length == 0 ) {
      $("#messageSpan").show().text("Please enter a value for the word verification above");
      return false;
    }
  }

  $("#submitButton").attr("disabled", "disabled");

  $.ajax({
    url: "../../api/account",
    data: $("#accountForm").serialize(),
    type: "post",
    success: function(data) {
      window.location.href = "../../dhis-web-commons-about/redirect.action";
    },
    error: function(jqXHR, textStatus, errorThrown) {
      var error = JSON.parse(jqXHR.responseText);
      $("#messageSpan").show().text(error.message);
      reloadRecaptcha();
      $("#submitButton").removeAttr("disabled");
    }
  });
}

function recaptchaEnabled() {
  return typeof Recaptcha !== 'undefined';
}

function reloadRecaptcha() {
  if( recaptchaEnabled() ) {
    Recaptcha.reload();
  }
}

login.changeLocale = function(locale) {
  $.get('accountStrings.action?loc=' + locale, function(json) {
    $('#create_new_account').html(json.create_new_account);
    $('#label_firstName').html(json.name);
    $('#firstName').attr("placeholder", json.first_name);
    $('#surname').attr("placeholder", json.last_name);
    $('#label_username').html(json.user_name);
    $('#label_password').html(json.password);
    $('#label_retypePassword').html(json.confirm_password);
    $('#label_email').html(json.email);
    $('#label_mobile_phone').html(json.mobile_phone);
    $('#label_employer').html(json.employer);
    $('#label_recaptchaDiv').html(json.prove_not_robot);
    $('#cant_read_words').html(json.cant_read_words);
    $('#submitButton').val(json.create);
  });
}
