"use strict";

/*
 * Copyright (c) 2004-2013, University of Oslo
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

dhis2.util.namespace( 'dhis2.storage' );

(function( $, window, document, undefined ) {
    if( typeof window.indexedDB === 'undefined' ) {
        window.indexedDB = window.webkitIndexedDB || window.mozIndexedDB || window.oIndexedDB || window.msIndexedDB;
    }

    if( typeof window.IDBKeyRange === 'undefined' ) {
        window.IDBKeyRange = window.webkitIDBKeyRange || window.msIDBKeyRange;
    }

    if( typeof window.IDBTransaction === 'undefined' ) {
        window.IDBTransaction = window.webkitIDBTransaction || window.msIDBTransaction
    }

    dhis2.storage.IndexedDBAdapter = function( options ) {
        if( !(this instanceof dhis2.storage.IndexedDBAdapter) ) {
            return new dhis2.storage.IndexedDBAdapter( options );
        }

        Object.defineProperties( this, {
            'name': {
                value: options.name,
                enumerable: true
            },
            'version': {
                value: options.version,
                enumerable: true
            },
            'objectStoreNames': {
                value: options.objectStores,
                enumerable: true
            },
            'keyPath': {
                value: options.keyPath,
                enumerable: true
            }
        } );

        return this;
    };

    Object.defineProperties( dhis2.storage.IndexedDBAdapter.prototype, {
        'open': {
            value: function() {
                var self = this;
                var deferred = $.Deferred();

                var request = window.indexedDB.open( self.name, self.version );

                request.onupgradeneeded = function( e ) {
                    self._db = e.target.result;

                    $.each( self.objectStoreNames, function( idx, item ) {
                        if( self._db.objectStoreNames.contains( item ) ) {
                            self._db.deleteObjectStore( item );
                        }
                    } );

                    $.each( self.objectStoreNames, function( idx, item ) {
                        self._db.createObjectStore( item );
                    } );
                };

                request.onsuccess = function( e ) {
                    self._db = e.target.result;
                    deferred.resolveWith( self );
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, e );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, e );
                };

                return deferred.promise();
            },
            enumerable: true
        },
        'set': {
            value: function( store, object ) {
                var self = this;
                var deferred = $.Deferred();

                if( typeof self._db === 'undefined' ) {
                    throw new Error( dhis2.storage.DATABASE_IS_NOT_OPEN );
                }

                if( typeof object === 'undefined' || typeof object[self.keyPath] === 'undefined' ) {
                    throw new Error( dhis2.storage.INVALID_OBJECT );
                }

                object = JSON.parse( JSON.stringify( object ) );

                var key = object[self.keyPath];
                delete object[self.keyPath];

                var tx = self._db.transaction( [ store ], "readwrite" );
                var objectStore = tx.objectStore( store );
                var request = objectStore.put( object, key );

                request.onsuccess = function() {
                    deferred.resolveWith( self, [ object ] );
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, e );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, e );
                };

                return deferred.promise();
            },
            enumerable: true
        },
        'setAll': {
            value: function( store, arr ) {
                var self = this;
                var deferred = $.Deferred();

                if( typeof self._db === 'undefined' ) {
                    throw new Error( dhis2.storage.DATABASE_IS_NOT_OPEN );
                }

                var storedCount = 0;

                var tx = self._db.transaction( [ store ], "readwrite" );
                var objectStore = tx.objectStore( store );

                function insertItem() {
                    if( storedCount < arr.length ) {
                        var object = arr[storedCount];
                        object = JSON.parse( JSON.stringify( object ) );

                        if( typeof object === 'undefined' || typeof object[self.keyPath] === 'undefined' ) {
                            throw new Error( dhis2.storage.INVALID_OBJECT );
                        }

                        var key = object[self.keyPath];
                        delete object[self.keyPath];

                        objectStore.put( object, key ).onsuccess = function() {
                            storedCount++;
                            insertItem();
                        };
                    } else {
                        deferred.resolveWith( self );
                    }
                }

                insertItem();

                return deferred.promise();
            },
            enumerable: true
        },
        'get': {
            value: function( store, key ) {
                var self = this;

                if( typeof self._db === 'undefined' ) {
                    throw new Error( dhis2.storage.DATABASE_IS_NOT_OPEN );
                }

                if( typeof key === 'undefined' ) {
                    throw new Error( dhis2.storage.INVALID_KEY );
                }

                var tx = self._db.transaction( [ store ], "readonly" );
                var objectStore = tx.objectStore( store );
                var request = objectStore.get( key );

                var deferred = $.Deferred();

                request.onsuccess = function( e ) {
                    var object = e.target.result;

                    if( typeof object !== 'undefined' ) {
                        object[self.keyPath] = key;
                    }

                    deferred.resolveWith( self, [ object ] );
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, e );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, e );
                };

                return deferred.promise();
            },
            enumerable: true
        },
        'getAll': {
            value: function( store, predicate ) {
                var self = this;
                var deferred = $.Deferred();

                var records = [];
                var filtered = typeof predicate === 'function';

                var tx = self._db.transaction( [ store ], "readonly" );
                var objectStore = tx.objectStore( store );
                var request = objectStore.openCursor();

                request.onsuccess = function( e ) {
                    var cursor = e.target.result;

                    if( cursor ) {
                        cursor.value[self.keyPath] = cursor.key;

                        if( filtered ) {
                            if( predicate( cursor.value ) ) {
                                records.push( cursor.value );
                            }
                        } else {
                            records.push( cursor.value );
                        }

                        cursor['continue']();
                    } else {
                        deferred.resolveWith( self, [ records ] );
                    }
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, e );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, e );
                };

                return deferred.promise();
            },
            enumerable: true
        },
        'getKeys': {
            value: function( store ) {
                var self = this;
                var deferred = $.Deferred();

                var keys = [];
                var tx;

                try {
                    tx = self._db.transaction([store], "readonly");
                } catch(e) {
                    console.log('Could not find store, returning empty keys.');
                    deferred.resolveWith(self, []);
                }

                var objectStore = tx.objectStore( store );
                var request = objectStore.openCursor();

                request.onsuccess = function( e ) {
                    var cursor = e.target.result;

                    if( cursor ) {
                        keys.push( cursor.key );
                        cursor['continue']();
                    } else {
                        deferred.resolveWith( self, [ keys ] );
                    }
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, e );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, e );
                };

                return deferred.promise();
            },
            enumerable: true
        },
        'remove': {
            value: function( store, key ) {
                var self = this;

                if( typeof self._db === 'undefined' ) {
                    throw new Error( dhis2.storage.DATABASE_IS_NOT_OPEN );
                }

                if( typeof key === 'undefined' ) {
                    throw new Error( dhis2.storage.INVALID_KEY );
                }

                var tx = self._db.transaction( [ store ], "readwrite" );
                var objectStore = tx.objectStore( store );
                var request = objectStore['delete']( key );

                var deferred = $.Deferred();

                request.onsuccess = function( e ) {
                    deferred.resolveWith( self );
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, e );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, e );
                };

                return deferred.promise();
            },
            enumerable: true
        },
        'removeAll': {
            value: function( store ) {
                var self = this;

                if( typeof self._db === 'undefined' ) {
                    throw new Error( DATABASE_IS_NOT_OPEN );
                }

                var tx = self._db.transaction( [ store ], "readwrite" );
                var objectStore = tx.objectStore( store );
                var request = objectStore.clear();

                var deferred = $.Deferred();

                request.onsuccess = function( e ) {
                    deferred.resolveWith( self );
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, e );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, e );
                };

                return deferred.promise();
            },
            enumerable: true
        },
        'contains': {
            value: function( store, key ) {
                var self = this;

                if( typeof self._db === 'undefined' ) {
                    throw new Error( dhis2.storage.DATABASE_IS_NOT_OPEN );
                }

                if( typeof key === 'undefined' ) {
                    throw new Error( dhis2.storage.INVALID_KEY );
                }

                var tx = self._db.transaction( [ store ], "readonly" );
                var objectStore = tx.objectStore( store );
                var request = objectStore.get( key );

                var deferred = $.Deferred();

                request.onsuccess = function( e ) {
                    deferred.resolveWith( self, [ e.target.result !== undefined ] );
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, e );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, e );
                };

                return deferred.promise();
            },
            enumerable: true
        },
        'count': {
            value: function( store, key ) {
                var self = this;

                if( typeof self._db === 'undefined' ) {
                    throw new Error( dhis2.storage.DATABASE_IS_NOT_OPEN );
                }

                if( typeof key === 'undefined' ) {
                    key = null;
                }

                var tx = self._db.transaction( [ store ], "readonly" );
                var objectStore = tx.objectStore( store );
                var request = objectStore.count( key );

                var deferred = $.Deferred();

                request.onsuccess = function( e ) {
                    deferred.resolveWith( self, [ e.target.result ] );
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, e );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, e );
                };

                return deferred.promise();
            },
            enumerable: true
        },
        'close': {
            value: function() {
                var deferred = $.Deferred();

                if( typeof idb._db === 'undefined' ) {
                    deferred.resolve();
                }

                idb._db.close();
                idb._db = undefined;

                deferred.resolve();

                return deferred.promise();
            },
            enumerable: true
        },
        'destroy': {
            value: function() {
                var self = this;
                var deferred = $.Deferred();

                if( typeof self._db !== 'undefined' ) {
                    self._db.close();
                }

                var request = window.indexedDB.deleteDatabase( self.name );

                request.onsuccess = function( e ) {
                    self._db = undefined;
                    deferred.resolveWith( self, [ e ] );
                };

                request.onerror = function( e ) {
                    console.log( 'error' );
                    deferred.rejectWith( self, [ e ] );
                };

                request.onblocked = function( e ) {
                    console.log( 'blocked' );
                    deferred.rejectWith( self, [ e ] );
                };

                return deferred.promise();
            },
            enumerable: true
        }
    } );

    Object.defineProperties( dhis2.storage.IndexedDBAdapter, {
        'adapterName': {
            value: 'IndexedDBAdapter',
            enumerable: true
        },
        'isSupported': {
            value: function() {
                return !!(window.indexedDB && window.IDBTransaction && window.IDBKeyRange);
            },
            enumerable: true
        }
    } );
})( jQuery, window, document );
