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

dhis2.util.namespace( 'dhis2.sharing' );

function loadSharingSettings(type, uid) {
  return $.ajax( {
    url: '../api/sharing',
    data: {
      type: type,
      id: uid
    },
    dataType: 'json'
  } );
}

function saveSharingSettings(type, uid, data) {
  return $.ajax( {
    url: '../api/sharing?type=' + type + '&id=' + uid,
    type: 'POST',
    dataType: 'text',
    contentType: 'application/json; charset=UTF-8',
    data: JSON.stringify( data )
  } );
}

var sharingSelectedItem;

function renderAccessTemplate(item) {
  var tmpl = jQuery( '#access-template' ).html();
  return _.template( tmpl, item );
}

function addAccessSelectedItem(e) {
  var tmpl_html = renderAccessTemplate( {
    id: sharingSelectedItem.id,
    type: sharingSelectedItem.type,
    label: sharingSelectedItem.label,
    label_can_view : window.i18n_can_view,
    label_can_edit_and_view : window.i18n_can_edit_and_view,
    access: "r-------"
  } );

  $( tmpl_html ).insertAfter( $( '#sharingAccessTable tbody tr' ).not( '[id]' ).last() );

  $( '#sharingSearch' ).val( '' );
  sharingSelectedItem = undefined;

  $( '#addAccess' ).attr( 'disabled', true );
}

function removeAccess(e) {
  e.preventDefault();
  $( this ).parent().parent().remove();
}

function clearAccesses() {
  $( '#sharingAccessTable tbody tr[id]' ).remove();
}

function setAccesses(userGroupAccesses, userAccesses) {
  clearAccesses();

  if ( userGroupAccesses ) {
    userGroupAccesses.reverse();

    $.each( userGroupAccesses, function(idx, item) {
      var tmpl_html = renderAccessTemplate( {
        label: item.name,
        id: item.id,
        type: 'userGroup',
        label_can_view : window.i18n_can_view,
        label_can_edit_and_view : window.i18n_can_edit_and_view,
        access: item.access
      } );

      $( tmpl_html ).insertAfter( $( '#sharingAccessTable tbody tr' ).not( '[id]' ).last() );
    } );
  }

  if ( userAccesses ) {
    userAccesses.reverse();

    $.each( userAccesses, function(idx, item) {
      var tmpl_html = renderAccessTemplate( {
        label: item.name,
        id: item.id,
        type: 'user',
        label_can_view : window.i18n_can_view,
        label_can_edit_and_view : window.i18n_can_edit_and_view,
        access: item.access
      } );

      $( tmpl_html ).insertAfter( $( '#sharingAccessTable tbody tr' ).not( '[id]' ).last() );
    } );
  }
}

function setPublicAccess(access) {
  $( '#sharingPublicAccess option' ).removeAttr( 'selected' ).each( function(idx, item) {
    if ( $( item ).val() == access ) {
      $( item ).attr( 'selected', true );
    }
  } );
}

function getPublicAccess() {
  return $( '#sharingPublicAccess' ).val();
}

function setExternalAccess(access) {
  if ( access ) {
    $( '#sharingExternalAccess' ).attr( 'checked', true );
  } else {
    $( '#sharingExternalAccess' ).removeAttr( 'checked' );
  }
}

function getExternalAccess() {
  return $( '#sharingExternalAccess' ).is( ':checked' );
}

function getAccesses() {
  var v = [];

  $( '#sharingAccessTable tbody tr[id]' ).each( function(idx, item) {
    var jqItem = $( item );

    var name = $( item ).find( '.sharingName' ).text();
    var access = $( item ).find( '.sharingAccess' ).val();

    v.push( {
      id: jqItem.attr( 'id' ),
      type: jqItem.attr( 'type' ),
      name: name,
      access: access
    } );
  } );

  return v;
}

function setCreatedBy(user) {
  if ( user && user.name ) {
    $( '#sharingUser' ).text( user.name );
  }
  else {
    $( '#sharingUser' ).text( "[None]" );
  }
}

function showSharingDialogWithContext(context) {
  // context always give type which starts with UpperCase
  var type = context.type.charAt( 0 ).toLowerCase() + context.type.slice( 1 );
  showSharingDialog( type, context.uid );
}

function showSharingDialog(type, uid) {
  loadSharingSettings( type, uid ).done( function(data) {
    setCreatedBy( data.object.user );
    setPublicAccess( data.object.publicAccess );
    setExternalAccess( data.object.externalAccess );
    setAccesses( data.object.userGroupAccesses, data.object.userAccesses );

    $( '#sharingName' ).text( data.object.name );

    if ( !data.meta.allowExternalAccess ) {
      $( '#sharingExternalAccess' ).attr( 'disabled', true );
    }

    if ( !data.meta.allowPublicAccess ) {
      $( '#sharingPublicAccess' ).attr( 'disabled', true );
    }

    $( '.removeUserGroupAccess' ).unbind( 'click' );
    $( document ).on( 'click', '.removeUserGroupAccess', removeAccess );
    $( '#addAccess' ).unbind( 'click' ).bind( 'click', addAccessSelectedItem );

    var buttons = {};

    buttons[i18n_cancel] = function() {
      $( '#sharingSearch' ).autocomplete( 'destroy' );
      $( this ).dialog( 'destroy' );
    };

    buttons[i18n_save] = function() {
      var me = $( this );

      data.object.publicAccess = getPublicAccess();
      data.object.externalAccess = getExternalAccess();

      var allAccesses = getAccesses();

      data.object.userGroupAccesses = allAccesses.filter( function(item) {
        return item.type === 'userGroup';
      } );

      data.object.userAccesses = allAccesses.filter( function(item) {
        return item.type === 'user';
      } );

      saveSharingSettings( type, uid, data ).done( function() {
        $( '#sharingSearch' ).autocomplete( 'destroy' );
        me.dialog( 'destroy' );
      } );
    };

    $( '#sharingSettings' ).dialog( {
      modal: true,
      resizable: false,
      width: 485,
      height: 555,
      buttons: buttons
    } );

    $( '#sharingSearch' ).autocomplete( {
      source: function(request, response) {
        $.ajax( {
          url: '../api/sharing/search',
          dataType: 'json',
          data: {
            key: request.term,
            pageSize: 20
          }
        } ).success( function(data) {
          var v = [];

          var allAccesses = getAccesses();
          var ug = allAccesses.filter( function(item) {
            return item.type === 'userGroup';
          } );

          var u = allAccesses.filter( function(item) {
            return item.type === 'user';
          } );

          console.log( 'ug', ug );
          console.log( 'u', u );

          if ( data.userGroups ) {
            $.each( data.userGroups, function(idx, item) {
              var d = {};

              d.id = item.id;
              d.type = 'userGroup';
              d.label = item.name;
              d.value = item.name;

              var found = false;

              $.each( ug, function(idx, item) {
                if ( item.id == d.id ) {
                  found = true;
                }
              } );

              if ( !found ) {
                v.push( d );
              }
            } );
          }

          if ( data.users ) {
            $.each( data.users, function(idx, item) {
              var d = {};

              d.id = item.id;
              d.type = 'user';
              d.label = item.name;
              d.value = item.name;

              var found = false;

              $.each( u, function(idx, item) {
                if ( item.id == d.id ) {
                  found = true;
                }
              } );

              if ( !found ) {
                v.push( d );
              }
            } );
          }

          response( v );
        } );
      },
      minLength: 2,
      select: function(event, ui) {
        sharingSelectedItem = ui.item;
        $( '#addAccess' ).removeAttr( 'disabled' );
      }
    } );
  } );
}
