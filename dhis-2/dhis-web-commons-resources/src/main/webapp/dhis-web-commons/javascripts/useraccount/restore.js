var validationRules = {
  rules: {
    code: {
      required: true
    },
    password: {
      required: true,
      rangelength: [8, 80],
      password: true
    },
    retypePassword: {
      required: true,
      equalTo: "#password"
    }
  }
};

$(document).ready(function() {
  $("#restoreForm").validate({
    rules: validationRules.rules,
    submitHandler: restoreSubmitHandler,
    errorPlacement: function(error, element) {
      element.parent("td").append("<br>").append(error);
    }
  });
});

function restoreSubmitHandler() {
  $("#submitButton").attr("disabled", "disabled");

  $.ajax({
    url: "../../api/account/restore",
    data: {
      username: $("#username").val(),
      token: $("#token").val(),
      code: $("#code").val(),
      password: $("#password").val()
    },
    type: "post",
    success: function(data) {
      $("#restoreForm").hide();
      $("#restoreSuccessMessage").fadeIn();
    },
    error: function(jqXHR, textStatus, errorThrown) {
      $("#submitButton").removeAttr("disabled");
      $("#restoreErrorMessage").fadeIn();
    }
  });
}