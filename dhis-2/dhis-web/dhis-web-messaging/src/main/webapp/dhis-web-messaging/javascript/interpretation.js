var currentPage = 0;
var pageLock = false;

$(function() {
  $(document).scroll(function() {
    isNextPage();
  });

  $("#interpretationFeed").load("getInterpretations.action", function() {
    $(".commentArea").autogrow();
  });
});

function expandComments( id ) {
  $("#comments" + id).children().show();
  $("#commentHeader" + id).hide();
}

function isNextPage() {
  var fromTop = $(document).scrollTop();
  var docHeight = $(document).height();
  var windowHeight = $(window).height();
  var threshold = parseInt(350);
  var remaining = parseInt(docHeight - ( fromTop + windowHeight ));

  if( remaining < threshold ) {
    loadNextPage();
  }
}

function loadNextPage() {
  if( pageLock == true ) {
    return false;
  }

  pageLock = true;
  currentPage++;

  $.get("getInterpretations.action", { page: currentPage }, function( data ) {
    $("#interpretationFeed").append(data);

    if( !isDefined(data) || $.trim(data).length == 0 ) {
      $(document).off("scroll");
    }

    pageLock = false;
    updateEventHandlers();
  });
}

function postComment( uid ) {
  var text = $("#commentArea" + uid).val();

  $("#commentArea" + uid).val("");

  var url = "../api/interpretations/" + uid + "/comments";

  var created = getCurrentDate();

  var gearBox = "<div class=\"gearDropDown\">\n  <span><i class=\"fa fa-gear\"></i> <i class=\"fa fa-caret-down\"></i></span>\n</div>\n";

  if( text.length && $.trim(text).length ) {
    $.ajax(url, {
      type: "POST",
      contentType: "text/html",
      data: $.trim(text),
      success: function( data, textStatus, request ) {
        var locationArray = request.getResponseHeader('Location').split('/');
        var commentUid = locationArray[locationArray.length - 1];

        var template =
          "<div class='interpretationComment' data-ip-comment-uid='" + commentUid + "'>" +
            "<div><div class=\"interpretationName\">" +
            "<a class=\"bold userLink\" href=\"profile.action?id=${userUid}\">${userName}</a>&nbsp;" +
            "<span class=\"grey\">${created}<\/span>" + gearBox + "<\/div><\/div>" +
            "<div class=\"interpretationText\">${text}<\/div>" +
            "</div>";

        $.tmpl(template, {
          "userId": currentUser.id,
          "userUid": currentUser.uid,
          "userName": currentUser.name,
          created: created,
          text: text }).appendTo("#comments" + uid);
      }
    });
  }
}

// DropDown Actions

function editIp( e ) {
  var jqActiveGearDropDown = jQuery('.gearDropDown.active');
  var isHeader = jqActiveGearDropDown.parents('.interpretationContent').length != 0;
  var isComment = jqActiveGearDropDown.parents('.interpretationCommentArea').length != 0;
  var jqInterpretation = jqActiveGearDropDown.parents('.interpretationContainer');
  var jqInterpretationComment = jqActiveGearDropDown.parents('.interpretationComment');

  var ipUid = jqInterpretation.data('ip-uid');
  var ipCommentUid = jqInterpretationComment.data('ip-comment-uid');

  if( isHeader ) {
    var jqTarget = jqInterpretation.find('.interpretationContent').find('.interpretationText');
    setupTextArea(ipUid, ipCommentUid,jqTarget );
  } else if( isComment ) {
    var jqTarget = jqInterpretationComment.find('.interpretationText');
    setupTextArea(ipUid, ipCommentUid,jqTarget );
  }
}

function setupTextArea( ipUid, ipCommentUid, $target ) {
  var oldContent = $target.html().trim();

  var textArea = jQuery("<textarea />")
    .css({ 'width': '100%', 'height': '80px' })
    .uniqueId()
    .val(oldContent);

  var container = jQuery("<div />")
    .uniqueId()
    .append(textArea);

  var cancelButton = jQuery("<button/>")
    .text(i18n_cancel)
    .on('click', function( e ) {
      $target.html(oldContent);
    });

  var saveButton = jQuery("<button/>")
    .text(i18n_save)
    .on('click', function( e ) {
      disableButtons();
      var content = textArea.val().trim();

      if( ipCommentUid ) {
        $.ajax({
          url: '../api/interpretations/' + ipUid + '/comments/' + ipCommentUid,
          contentType: 'text/plain; charset=UTF-8',
          type: 'PUT',
          data: content
        }).done(function() {
          $target.html(content);
        }).error(function() {
          setHeaderDelayMessage(i18n_could_not_save_interpretation);
        }).always(function() {
          enableButtons();
        });
      } else {
        $.ajax({
          url: '../api/interpretations/' + ipUid,
          contentType: 'text/plain; charset=UTF-8',
          type: 'PUT',
          data: content
        }).done(function() {
          $target.html(content);
        }).error(function() {
          setHeaderDelayMessage(i18n_could_not_save_interpretation);
        }).always(function() {
          enableButtons();
        });
      }
    });

  function enableButtons() {
    saveButton.removeAttr('disabled');
    cancelButton.removeAttr('disabled');
  }

  function disableButtons() {
    saveButton.attr('disabled', true);
    cancelButton.attr('disabled', true);
  }

  container.append(cancelButton);
  container.append(saveButton);

  $target.html(container);

  textArea.focus();
}

function deleteIp( e ) {
  var jqActiveGearDropDown = jQuery('.gearDropDown.active');
  var isHeader = jqActiveGearDropDown.parents('.interpretationContent').length != 0;
  var isComment = jqActiveGearDropDown.parents('.interpretationCommentArea').length != 0;
  var jqInterpretation = jqActiveGearDropDown.parents('.interpretationContainer');
  var jqInterpretationComment = jqActiveGearDropDown.parents('.interpretationComment');

  var ipUid = jqInterpretation.data('ip-uid');
  var ipCommentUid = jqInterpretationComment.data('ip-comment-uid');

  if( isHeader ) {
    jQuery.ajax({
      url: '../api/interpretations/' + ipUid,
      type: 'DELETE'
    }).done(function() {
      jqInterpretation.remove();
    }).error(function() {
      setHeaderDelayMessage(i18n_could_not_delete_interpretation);
    });
  } else if( isComment ) {
    jQuery.ajax({
      url: '../api/interpretations/' + ipUid + '/comments/' + ipCommentUid,
      type: 'DELETE'
    }).done(function() {
      jqInterpretationComment.remove();
    }).error(function() {
      setHeaderDelayMessage(i18n_could_not_delete_interpretation_comment);
    });
  }
}

function updateEventHandlers() {
    var dropDown = jQuery('.dropDown');

    dropDown.off('click.dropdown').on('click.dropdown', 'li', function( e ) {
      var jqTarget = jQuery(e.target).closest('li');
      var targetFn = dhis2.contextmenu.utils.findInScope(window)(jqTarget.data('target-fn'));

      if( typeof targetFn !== 'undefined' ) {
        targetFn(e);
      }
    });

    jQuery('.interpretationContainer').off('click').on('click', '.gearDropDown', function( e ) {
      var jqTarget = jQuery(e.target).closest('.gearDropDown');
      jQuery('.gearDropDown').removeClass('active');

      if(dropDown.is(':visible')) {
        dropDown.hide();
        return false;
      }

      jqTarget.addClass('active');
      dropDown.show();
      dropDown.css({
        top: jqTarget.offset().top + jqTarget.innerHeight(),
        left: jqTarget.offset().left - 34
      });

      return false;
    });

    jQuery(document).off('click').on('click', function() {
      if( dropDown.is(":visible") ) {
        jQuery('.gearDropDown').removeClass('active');
        dropDown.hide();
      }
    });

    $(document).keyup(function( e ) {
      if( e.keyCode == 27 ) {
        jQuery('.gearDropDown').removeClass('active');
        dropDown.hide();
      }
    });
}
