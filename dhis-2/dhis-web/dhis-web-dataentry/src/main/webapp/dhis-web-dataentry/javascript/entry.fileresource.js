( function ( $ ) {
    $.fn.fileEntryField = function() {
        var $field = $( this );

        var $input = $field.find( 'input[class="entryfileresource-input"]');
        var $displayField = $field.find( '.upload-field' );

        var $button = $field.find( '.upload-button' );

        var $fileInput = $field.find( 'input[type=file]' );

        var $fileinfo = $field.find( '.upload-fileinfo' );
        var $fileinfoName = $fileinfo.find( '.upload-fileinfo-name' );
        var $fileinfoSize = $fileinfo.find( '.upload-fileinfo-size' );

        var $progress = $field.find( '.upload-progress' );
        var $progressBar = $progress.find( '.upload-progress-bar' );
        var $progressInfo = $progress.find( '.upload-progress-info' );

        var id = $field.attr( 'id' );

        var split = dhis2.de.splitFieldId( id );

        var dataElementId = split.dataElementId;
        var optionComboId = split.optionComboId;
        var orgUnitid = split.organisationUnitId;
        var periodId = $( '#selectedPeriodId' ).val();

        var formData = {
            'de': dataElementId,
            'co': optionComboId,
            'ou': orgUnitid,
            'pe': periodId
        };

        var deleteFileDataValue = function() {
            var postData = formData;
            postData.value = '';

            $.ajax( {
                url: '../api/dataValues',
                type: 'POST',
                dataType: 'json',
                data: postData,
                success: function() {
                    $fileinfoName.text( '' );
                    $fileinfoSize.text( '' );
                    $displayField.css( 'background-color', '' );
                    $input.val( '' );
                    setButtonUpload();
                },
                error: function( data ) {
                    console.log( data.errorThrown );
                }
            } );
        };

        var setButtonDelete = function() {
            $button.button( {
                text: false,
                icons: {
                    primary: 'fa fa-trash-o'
                }
            } );
            $button.unbind( 'click' );
            $button.on( 'click', function() {
                $( '#fileDeleteConfirmationDialog' ).dialog( {
                    title: i18n_confirm_deletion,
                    resizable: false,
                    height: 140,
                    modal: true,
                    buttons: {
                        'Delete': function() {
                            deleteFileDataValue();
                            $( this ).dialog( 'close' );
                        },
                        Cancel: function() {
                            $( this ).dialog( 'close' );
                        }
                    }
                } );
            } );
            $button.button( isDisabled( $field ) ? 'disable' : 'enable' );
        };

        var setButtonUpload = function() {
            $button.button( {
                text: false,
                icons: {
                    primary: 'fa fa-upload'
                }
            } );
            $button.unbind( 'click' );
            $button.on( 'click', function()
            {
                $fileInput.click();
            } );
            $button.button( isDisabled( $field ) ? 'disable' : 'enable' );
        };

        var setButtonBlocked = function() {
            $button.button( {
                text: false,
                icons: {
                    primary: 'fa fa-ban'
                }
            } );
            $button.button( 'disable' );
        };

        var resetAndHideProgress = function() {
            $progressBar.toggleClass( 'upload-progress-bar-complete', true );
            $progressBar.css( 'width', 0 );
            $progress.hide();
        };

        var onFileDataValueSavedSuccess = function( fileResource ) {
            var name = fileResource.name;
            var size = '(' + filesize( fileResource.contentLength ) + ')';

            $fileinfoName.text( name );
            $fileinfoSize.text( size );
            $fileinfo.show();
            $displayField.css( 'background-color', dhis2.de.cst.colorYellow );
            resetAndHideProgress();

            function onFileResourceConfirmedStored() {
                $fileinfoName.text( '' );

                $( '<a>', {
                    text: name,
                    title: name,
                    target: '_blank',
                    href: '../api/dataValues/files?' + $.param( formData )
                } ).appendTo( $fileinfoName );

                $displayField.css( 'background-color', dhis2.de.cst.colorGreen );

                setButtonDelete();
                $button.button( 'enable' );
            }

            function pollForFileResourceStored() {
                $.ajax( {
                    url: '../api/fileResources/' + fileResource.id,
                    type: 'GET',
                    dataType: 'json'
                } ).done( function( data, textStatus, jqXHR ) {
                    if ( data.storageStatus != 'STORED' ) {
                        setTimeout( pollForFileResourceStored, 4000 /* 4 sec polling time */ );
                    } else {
                        onFileResourceConfirmedStored();
                    }
                } ).fail( function( jqXHR, textStatus, errorThrown ) {
                    // Really shouldn't happen...
                    $displayField.css( 'background-color', dhis2.de.cst.colorRed );
                    throw 'Checking storage status of file failed: ' + errorThrown;
                } );
            }

            if ( fileResource.storageStatus == 'STORED' ) {
                onFileResourceConfirmedStored();
            } else {
                setTimeout( pollForFileResourceStored, 1500 );
            }
        };

        var updateProgress = function( loaded, total ) {
            var percent = parseInt( loaded / total * 100, 10 );
            $progressBar.css( 'width', percent + '%' );
            $progressInfo.text( percent + '%' );
        };

        var disableField = function() {
            $button.button( 'disable' );
            $displayField.toggleClass( 'upload-field-disabled', true );
        };

        var enableField = function() {
            $button.button( 'enable' );
            $displayField.toggleClass( 'upload-field-disabled', false );
        };

        $( document ).on( dhis2.de.event.dataValuesLoaded, function() {
            ( !$input.val() ) ? setButtonUpload() : setButtonDelete();
        } );

        $( document ).on( "dhis2.offline", disableField );
        $( document ).on( "dhis2.online",  enableField );

        // Init
        setButtonBlocked();

        $fileInput.fileupload( {
            url: '../api/fileResources',
            paramName: 'file',
            multipart: true,
            replaceFileInput: false,
            progressInterval: 250, /* ms */
            start: function( e ) {
                $button.button( 'disable' );
                $progressBar.toggleClass( 'upload-progress-bar-complete', false );
                $fileinfo.hide();
                $progress.show();
            },
            progress: function( e, data ) {
                updateProgress( data.loaded, data.total );
            },
            fail: function( e, data ) {
                console.error( data.errorThrown );
                $displayField.css( 'background-color', dhis2.de.cst.colorRed );
                setHeaderDelayMessage( i18n_file_upload_failed );
                setButtonUpload();
            },
            done: function( e, data ) {
                var fileResource = data.result.response.fileResource;
                $input.val( fileResource.id );

                saveFileResource( dataElementId, optionComboId, $input.attr( 'id' ), fileResource, function() {
                    onFileDataValueSavedSuccess( fileResource );
                } );
            }
        } );

        function isDisabled( jqField ) {
            var value = jqField.data( 'disabled' );
            return value == 'undefined' ? false : value;
        }
    };
} )( jQuery );
