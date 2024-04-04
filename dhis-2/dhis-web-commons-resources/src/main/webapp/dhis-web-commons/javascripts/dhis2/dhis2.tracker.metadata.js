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

dhis2.util.namespace('dhis2.tracker');

dhis2.tracker.chunk = function( array, size ){
	if( !array.length || !size || size < 1 ){
		return []
	}
	
	var groups = [];
	var chunks = array.length / size;
	for (var i = 0, j = 0; i < chunks; i++, j += size) {
        groups[i] = array.slice(j, j + size);
    }
	
    return groups;
}

dhis2.tracker.getTrackerMetaObjects = function( programs, objNames, url, filter )
{
    if( !programs || !programs.programIds || programs.programIds.length === 0 ){
        return;
    }       

    filter = filter + '[' + programs.programIds.toString() + ']';
    
    var def = $.Deferred();
    
    $.ajax({
        url: url,
        type: 'GET',
        data:filter
    }).done( function(response) {
        
        def.resolve( {programs: programs, self: response[objNames], programIds: programs.programIds} );
        
    }).fail(function(){
        def.resolve( null );
    });
    
    return def.promise();
    
};

dhis2.tracker.checkAndGetTrackerObjects  = function( obj, store, url, filter, db )
{
    if( !obj || !obj.programs || !obj.programIds || !obj.self || !db ){
        return;
    }
    
    var mainDef = $.Deferred();
    var mainPromise = mainDef.promise();

    var def = $.Deferred();
    var promise = def.promise();

    var builder = $.Deferred();
    var build = builder.promise();

    var ids = [];
    _.each( _.values( obj.self ), function ( obj) {
        build = build.then(function() {
            var d = $.Deferred();
            var p = d.promise();
            db.get(store, obj.id).done(function(o) {
                //if(!o) {                    
                    ids.push( obj.id );
                //}
                d.resolve();
            });

            return p;
        });
    });

    build.done(function() {
        def.resolve();
        promise = promise.done( function () {
            
            if( ids && ids.length > 0 ){
                var _ids = ids.toString();
                _ids = '[' + _ids + ']';
                filter = filter + '&filter=id:in:' + _ids + '&paging=false';
                mainPromise = mainPromise.then( dhis2.tracker.getTrackerObjects( store, store, url, filter, 'idb', db ) );
            }
            
            mainDef.resolve( obj.programs, obj.programIds );
        } );
    }).fail(function(){
        mainDef.resolve( null );
    });

    builder.resolve();

    return mainPromise;
};

dhis2.tracker.getTrackerObjects = function( store, objs, url, filter, storage, db )
{
    var def = $.Deferred();

    $.ajax({
        url: url,
        type: 'GET',
        data: filter
    }).done(function(response) {
        if(response[objs]){
            if(storage === 'idb'){
                db.setAll( store, response[objs] );                
            }
            if(storage === 'localStorage'){                
                localStorage[store] = JSON.stringify(response[objs]);
            }            
            if(storage === 'sessionStorage'){
                var SessionStorageService = angular.element('body').injector().get('SessionStorageService');
                SessionStorageService.set(store, response[objs]);
            }
        }
        
        if(storage === 'temp'){
            def.resolve(response[objs] ? response[objs] : []);
        }
        else{
            def.resolve();
        }    
    }).fail(function(){
        def.resolve( null );
    });

    return def.promise();
};

dhis2.tracker.getTrackerObject = function( id, store, url, filter, storage, db )
{
    var def = $.Deferred();
    
    if(id){
        url = url + '/' + id + '.json';
    }
        
    $.ajax({
        url: url,
        type: 'GET',            
        data: filter
    }).done( function( response ){
        if(storage === 'idb'){
            if( response && response.id) {
                db.set( store, response );
            }
        }
        if(storage === 'localStorage'){
            localStorage[store] = JSON.stringify(response);
        }            
        if(storage === 'sessionStorage'){
            var SessionStorageService = angular.element('body').injector().get('SessionStorageService');
            SessionStorageService.set(store, response);
        } 
        
        def.resolve();
    }).fail(function(){
        def.resolve();
    });
    
    return def.promise();
};

dhis2.tracker.getBatches = function( ids, batchSize, data, store, objs, url, filter, storage, db )
{
    if( !ids || !ids.length || ids.length < 1){
        return;
    }
    
    var batches = dhis2.tracker.chunk( ids, batchSize );

    var mainDef = $.Deferred();
    var mainPromise = mainDef.promise();

    var def = $.Deferred();
    var promise = def.promise();

    var builder = $.Deferred();
    var build = builder.promise();
    
    _.each( _.values( batches ), function ( batch ) {        
        promise = promise.then(function(){
            return dhis2.tracker.fetchBatchItems( batch, store, objs, url, filter, storage, db );
        });
    });

    build.done(function() {
        def.resolve();
        promise = promise.done( function () {
            mainDef.resolve( data );
        } );        
        
    }).fail(function(){
        mainDef.resolve( null );
    });

    builder.resolve();

    return mainPromise;
};

dhis2.tracker.fetchBatchItems = function( batch, store, objs, url, filter, storage, db )
{   
    var ids = '[' + batch.toString() + ']';             
    filter = filter + '&filter=id:in:' + ids;    
    return dhis2.tracker.getTrackerObjects( store, objs, url, filter, storage, db );    
};
