"use strict";

/*
 * Copyright (c) 2004-2014, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the HISP project nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
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

dhis2.util.namespace('dhis2.sharing');

function loadSharingSettings( type, uid ) {
  return $.ajax({
    url: '../api/sharing',
    data: {
      type: type,
      id: uid
    },
    dataType: 'json'
  });
}

function saveSharingSettings( type, uid, data ) {
  return $.ajax({
    url: '../api/sharing?type=' + type + '&id=' + uid,
    type: 'POST',
    dataType: 'text',
    contentType: 'application/json; charset=UTF-8',
    data: JSON.stringify(data)
  });
}

var sharingSelectedItem;

function renderUserGroupAccessTemplate( item ) {
  var tmpl = jQuery('#user-group-access-template').html();
  return _.template(tmpl, item);
}

function addUserGroupAccessSelectedItem( e ) {
  var tmpl_html = renderUserGroupAccessTemplate({
    label: sharingSelectedItem.label,
    id: sharingSelectedItem.id,
    access: "r-------"
  });

  $(tmpl_html).insertAfter($('#sharingAccessTable tbody tr').not('[id]').last());

  $('#sharingFindUserGroup').val('');
  sharingSelectedItem = undefined;

  $('#addUserGroupAccess').attr('disabled', true);
}

function removeUserGroupAccess( e ) {
  e.preventDefault();
  $(this).parent().parent().remove();
}

function clearUserGroupAccesses() {
  $('#sharingAccessTable tbody tr[id]').remove();
}

function setUserGroupAccesses( userGroupAccesses ) {
  clearUserGroupAccesses();

  if( userGroupAccesses ) {
    userGroupAccesses.reverse();

    $.each(userGroupAccesses, function( idx, item ) {
      var tmpl_html = renderUserGroupAccessTemplate({
        label: item.name,
        id: item.id,
        access: item.access
      });

      $(tmpl_html).insertAfter($('#sharingAccessTable tbody tr').not('[id]').last());
    });
  }
}

function setPublicAccess( access ) {
  $('#sharingPublicAccess option').removeAttr('selected').each(function( idx, item ) {
    if( $(item).val() == access ) {
      $(item).attr('selected', true);
    }
  });
}

function getPublicAccess() {
  return $('#sharingPublicAccess').val();
}

function setExternalAccess( access ) {
  if( access ) {
    $('#sharingExternalAccess').attr('checked', true);
  } else {
    $('#sharingExternalAccess').removeAttr('checked');
  }
}

function getExternalAccess() {
  return $('#sharingExternalAccess').is(':checked');
}

function getUserGroupAccesses() {
  var v = [];

  $('#sharingAccessTable tbody tr[id]').each(function( idx, item ) {
    var jqItem = $(item);

    var groupName = $(item).find('.sharingGroupName').text();
    var groupAccess = $(item).find('.sharingGroupAccess').val();

    v.push({
      id: jqItem.attr('id'),
      name: groupName,
      access: groupAccess
    });
  });

  return v;
}

function setCreatedBy( user ) {
  if( user && user.name ) {
    $('#sharingUser').text(user.name);
  }
  else {
    $('#sharingUser').text("[None]");
  }
}

function showSharingDialogWithContext( context ) {
  // context always give type which starts with UpperCase
  var type = context.type.charAt(0).toLowerCase() + context.type.slice(1);
  showSharingDialog(type, context.uid);
}

function showSharingDialog( type, uid ) {
  loadSharingSettings(type, uid).done(function( data ) {
    setCreatedBy(data.object.user);
    setPublicAccess(data.object.publicAccess);
    setExternalAccess(data.object.externalAccess);
    setUserGroupAccesses(data.object.userGroupAccesses);

    $('#sharingName').text(data.object.name);

    if( !data.meta.allowExternalAccess ) {
      $('#sharingExternalAccess').attr('disabled', true);
    }

    if( !data.meta.allowPublicAccess ) {
      $('#sharingPublicAccess').attr('disabled', true);
    }

    $('.removeUserGroupAccess').unbind('click');
    $(document).on('click', '.removeUserGroupAccess', removeUserGroupAccess);
    $('#addUserGroupAccess').unbind('click').bind('click', addUserGroupAccessSelectedItem);

    $('#sharingSettings').dialog({
      modal: true,
      resizable: false,
      width: 485,
      height: 555,
      buttons: {
        'Cancel': function() {
          $('#sharingFindUserGroup').autocomplete('destroy');
          $(this).dialog('destroy');
        },
        'Save': function() {
          var me = $(this);

          data.object.publicAccess = getPublicAccess();
          data.object.externalAccess = getExternalAccess();
          data.object.userGroupAccesses = getUserGroupAccesses();

          saveSharingSettings(type, uid, data).done(function() {
            $('#sharingFindUserGroup').autocomplete('destroy');
            me.dialog('destroy');
          });
        }
      }
    });

    $('#sharingFindUserGroup').autocomplete({
      source: function( request, response ) {
        $.ajax({
          url: '../api/sharing/search',
          dataType: 'json',
          data: {
            key: request.term,
            pageSize: 20
          }
        }).success(function( data ) {
          var v = [];
          var u = getUserGroupAccesses();

          if( data.userGroups ) {
            $.each(data.userGroups, function( idx, item ) {
              var d = {};

              d.label = item.name;
              d.value = item.name;
              d.id = item.id;

              var found = false;

              $.each(u, function( idx, item ) {
                if( item.id == d.id ) {
                  found = true;
                }
              });

              if( !found ) {
                v.push(d);
              }
            });
          }

          response(v);
        });
      },
      minLength: 2,
      select: function( event, ui ) {
        sharingSelectedItem = ui.item;
        $('#addUserGroupAccess').removeAttr('disabled');
      }
    });
  });
}
