/* global angular */

'use strict';

/* Services */

var cacheCleanerServices = angular.module('cacheCleanerServices', ['ngResource']);

/*Orgunit service for local db */
cacheCleanerServices.service('idbStorageService', function ($window, $q) {

    var indexedDB = $window.indexedDB;
    
    var db = null;

    var open = function (dbName) {
        var deferred = $q.defer();

        var request = indexedDB.open(dbName);

        request.onsuccess = function (e) {
            db = e.target.result;
            deferred.resolve();
        };

        request.onerror = function () {
            deferred.reject();
        };

        return deferred.promise;
    };
    
    var dbExists = function(dbName){
        
        var deferred = $q.defer();

        var request = indexedDB.open(dbName);
        
        var existed = true;
        
        request.onsuccess = function (e) {
            request.result.close();
            
            if(!existed){
                indexedDB.deleteDatabase(dbName);
            } 
            
            deferred.resolve( existed );
        };

        request.onerror = function () {
            deferred.reject();
        };
        
        request.onupgradeneeded = function () {
            existed = false;
        };
        
        return deferred.promise;
    };
    
    var getObjectStores = function(dbName){
        
        var deferred = $q.defer();

        var request = indexedDB.open(dbName);
        
        request.onsuccess = function (e) {
            var db = e.target.result;
            deferred.resolve( db.objectStoreNames );
        };

        request.onerror = function () {
            deferred.reject();
        };
        return deferred.promise;
    };
    
    var deleteDb = function(dbName){
        
        var deferred = $q.defer();

        var request = indexedDB.deleteDatabase(dbName);
        
        request.onsuccess = function (e) {
            deferred.resolve( true );
        };

        request.onerror = function (e) {
            console.log('Error in deleting db: ', e);
            deferred.resolve( false );
        };
        return deferred.promise;
    };
    
    return {
        open: open,
        deleteDb: deleteDb,
        dbExists: dbExists,
        getObjectStores: getObjectStores
    };
});
