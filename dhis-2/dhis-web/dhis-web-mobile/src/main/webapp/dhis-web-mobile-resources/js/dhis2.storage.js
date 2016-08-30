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

var dhis2 = dhis2 || {};
dhis2['storage'] = dhis2['storage'] || {};

dhis2.storage.FormManager = function ( args ) {
    this._organisationUnits = undefined;
    this._forms = undefined;
    this._id = _.uniqueId('form-manager');
};

dhis2.storage.FormManager.prototype.getMetaData = function () {
    return $.ajax({
        url         : '../api/currentUser/assignedDataSets?optionSets=true',
        dataType    : 'json',
        cache       : false
    }).success(function ( data ) {
        // clear out old localStorage, some phones doesn't like it when you overwrite old keys
        localStorage.removeItem('mobileOrganisationUnits');
        localStorage.removeItem('mobileForms');
        localStorage.removeItem('mobileOptionSets');

        if( data.organisationUnits ) {
            localStorage.setItem('mobileOrganisationUnits', JSON.stringify(data.organisationUnits));
        } else {
            localStorage.setItem('mobileOrganisationUnits', JSON.stringify({}));
        }

        if( data.forms ) {
            localStorage.setItem('mobileForms', JSON.stringify(data.forms));
        } else {
            localStorage.setItem('mobileForms', JSON.stringify({}));
        }

        if( data.optionSets ) {
            localStorage.setItem('mobileOptionSets', JSON.stringify(data.optionSets));
        } else {
            localStorage.setItem('mobileOptionSets', JSON.stringify({}));
        }
    });
};

dhis2.storage.FormManager.prototype.needMetaData = function () {
    return this.organisationUnits() === undefined || this.forms() === undefined;
};

dhis2.storage.FormManager.prototype.organisationUnits = function () {
    if ( this._organisationUnits === undefined ) {
        var organisationUnits = localStorage.getItem('mobileOrganisationUnits');

        if( organisationUnits != null && organisationUnits != "null" ) {
            this._organisationUnits = JSON.parse(organisationUnits);
        }
    }

    return this._organisationUnits;
};

dhis2.storage.FormManager.prototype.organisationUnit = function (id) {
    return this.organisationUnits()[id];
};

dhis2.storage.FormManager.prototype.dataSets = function (id) {
    return this.organisationUnit(id).dataSets;
};

dhis2.storage.FormManager.prototype.optionSets = function() {
    if( this._optionSets === undefined ) {
        var optionSets = localStorage.getItem('mobileOptionSets');

        if( optionSets != null && optionSets != "null" ) {
            this._optionSets = JSON.parse(optionSets);
        }
    }

    return this._optionSets;
};

dhis2.storage.FormManager.prototype.optionSet = function( id ) {
  return this.optionSets()[id];
};

dhis2.storage.FormManager.prototype.forms = function () {
    if( this._forms === undefined ) {
        var form = localStorage.getItem('mobileForms');

        if( form != null && form != "null") {
            this._forms = JSON.parse( form );
        }
    }

    return this._forms;
};

dhis2.storage.FormManager.prototype.form = function ( id ) {
    return this.forms()[id]
};

dhis2.storage.FormManager.prototype.dataValueSets = function() {
    var dataValueSets = localStorage.getItem('mobileDataValueSets');

    if( dataValueSets != null && dataValueSets != "null" && dataValueSets != "[]" )
    {
        dataValueSets = JSON.parse( dataValueSets );
    } else {
        dataValueSets = {};
    }

    return dataValueSets;
};

dhis2.storage.makeUploadDataValueSetRequest = function( dataValueSet ) {
    return $.ajax({
        url         : '../api/dataValueSets',
        type        : 'POST',
        cache       : false,
        contentType : 'application/json',
        data        : JSON.stringify( dataValueSet )
    });
};

dhis2.storage.getUniqueKey = function( dataValueSet ) {
    return dataValueSet.orgUnit + '-' + dataValueSet.dataSet + '-' + dataValueSet.period;
};

dhis2.storage.FormManager.prototype.getDataValueSetValues = function( dataValueSet ) {
    var dataValueSets = this.dataValueSets();
    return dataValueSets[ dhis2.storage.getUniqueKey( dataValueSet )];
};

dhis2.storage.FormManager.prototype.saveDataValueSet = function( dataValueSet ) {
    var dataValueSets = this.dataValueSets();

    return dhis2.storage.makeUploadDataValueSetRequest( dataValueSet ).error(function() {
        // add to local dataValueSets
        dataValueSets[dhis2.storage.getUniqueKey(dataValueSet)] = dataValueSet;

        // delete old values
        localStorage.removeItem('mobileDataValueSets');
        localStorage.setItem('mobileDataValueSets', JSON.stringify( dataValueSets ));
    });
};

dhis2.storage.FormManager.prototype.uploadDataValueSets = function() {
    var dataValueSets = this.dataValueSets();
    var deferreds = [];

    _.each(dataValueSets, function( value, key ) {
        deferreds.push(dhis2.storage.makeUploadDataValueSetRequest( value ).success(function() {
                delete dataValueSets[key];
            })
        );
    });

    return $.when.apply( null, deferreds ).always(function() {
        // delete old values
        localStorage.removeItem('mobileDataValueSets');
        localStorage.setItem('mobileDataValueSets', JSON.stringify( dataValueSets ));
    });
};

// global storage manager instance
(function () {
    window.fm = new dhis2.storage.FormManager();
}).call();
