/**
 * Form setup for add and update forms
 */
( function( qs ) {
    jQuery( document ).ready( function() {
        var params = qs( '#params' );
        var focusedTextArea = qs( '#subjectTemplate' );
        var recipientSelector = qs( '#notificationRecipient' );
        var userGroup = qs( '#userGroup' );
        var userGroupContainer = qs( '#userGroupContainer' );
        var notificationTrigger = qs( '#notificationTrigger' );
        var daysContainer = qs( '#daysContainer' );
        var days = qs( '#days' );
        var subjectTemplateTextArea = qs( '#subjectTemplate' );
        var messageTemplateTextArea = qs( '#messageTemplate' );
        var saveButton = qs( '#save' );
        var cancelButton = qs( '#cancel' );
        var programId = qs( '#programId' ).value;
        var programUid = qs( '#programUid' ).value;

        var isUpdate = Boolean( qs( '#isUpdate' ).value );

        var templateUid = isUpdate ? qs( '#templateUid' ).value : undefined;

        // Event handlers

        subjectTemplateTextArea.onfocus = function() {
            focusedTextArea = subjectTemplateTextArea;
        };

        messageTemplateTextArea.onfocus = function() {
            focusedTextArea = messageTemplateTextArea;
        };

        // Click handlers

        recipientSelector.onchange = function( e ) {
            if ( recipientSelector.value === 'USER_GROUP' ) {
                userGroupContainer.style.display = 'table-row';
                userGroup.disabled = false;
            } else {
                userGroupContainer.style.display = 'none';
                userGroup.value = "";
                userGroup.disabled = true;
            }
        };

        notificationTrigger.onchange = function( e ) {
            if ( notificationTrigger.value === 'ENROLLMENT' || notificationTrigger.value == 'COMPLETION' ) {
                daysContainer.style.display = 'none';
            } else {
                daysContainer.style.display = 'table-row';
                days.value = undefined;
            }
        };

        params.ondblclick = function() {
            insertTextCommon( focusedTextArea.id, params.value );
        };

        cancelButton.onclick = function() {
            window.location.href = 'programNotification.action?id=' + programId;
        };

        saveButton.onclick = function() {
            var json = formAsJson();
            isUpdate ? update( json ) : save( json );
        };

        // Internal

        function getSelectedDeliveryChannels() {
            return Array.prototype.slice.call(
                document.forms[ 'deliveryChannelsForm' ].elements[ 'deliveryChannels[]' ] )
                .filter( function( cb ) { return cb.checked; } )
                .map( function( cb ) { return cb.value; } );
        };

        function getUserGroup() {
            var uid = qs( '#userGroup' ).value || undefined;
            return ( uid === undefined ) ? undefined : { 'id' : uid };
        };

        function getScheduledDays() {
            return ( days.value || 0 ) * ( qs( '#daysModifier' ).value );
        };

        function formAsJson() {
            return {
                name : qs( '#name' ).value || '',
                notificationTrigger : qs( '#notificationTrigger' ).value,
                relativeScheduledDays : getScheduledDays(),
                notificationRecipient : qs( '#notificationRecipient' ).value,
                recipientUserGroup : getUserGroup(),
                deliveryChannels : getSelectedDeliveryChannels(),
                subjectTemplate : qs( '#subjectTemplate' ).value,
                messageTemplate : qs( '#messageTemplate' ).value
            };
        };

        function update( json ) {
            jQuery.ajax( {
                url : '../api/programNotificationTemplates/' + templateUid,
                dataType : 'json',
                data : JSON.stringify( json ),
                contentType : ' application/json',
                type : 'PUT'
            } ).done( onSaveDone ).fail( onSaveFail );
        };

        function save( json ) {
            jQuery.ajax( {
                url : '../api/programNotificationTemplates',
                dataType : 'json',
                data : JSON.stringify( json ),
                contentType : 'application/json',
                type : 'POSt'
            } ).then( function( result ) {
                return saveToProgram( programUid, result.response.uid );
            } ).done( function( result ) {
                onSaveDone()
            } )
            .fail( function( jqXhr, textStatus, error ) {
                onSaveFail( jqXhr, textStatus, error );
            } );
        };

        function saveToProgram( uidOfProgram, uidOfTemplate ) {
            return jQuery.ajax( {
                type: 'POST',
                url: '../api/programs/' + programUid + '/notificationTemplates/' + uidOfTemplate,
            } );
        };

        function onSaveFail( jqXhr, textStatus, error ) {
            var json = jQuery.parseJSON( jqXhr.responseText );
            var errorMessage = json.response.errorReports[0].message;

            setHeaderDelayMessage( errorMessage || ( textStatus + ': ' + error ) );
        };

        function onSaveDone() {
            window.location.href = 'programNotification.action?id=' + programId;
        };
    } );
} )( document.querySelector.bind( document ) );