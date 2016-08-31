/* global angular, dhis2 */

'use strict';

/* Services */

var eventCaptureServices = angular.module('eventCaptureServices', ['ngResource'])

.factory('ECStorageService', function(){
    var store = new dhis2.storage.Store({
        name: 'dhis2ec',
        adapters: [dhis2.storage.IndexedDBAdapter, dhis2.storage.DomSessionStorageAdapter, dhis2.storage.InMemoryAdapter],
        objectStores: ['programs', 'optionSets', 'events', 'programValidations', 'programRules', 'programRuleVariables', 'programIndicators', 'ouLevels', 'constants', 'dataElements']
    });
    return{
        currentStore: store
    };
})

.factory('OfflineECStorageService', function($http, $q, $rootScope, ECStorageService){
    return {        
        hasLocalData: function() {
            var def = $q.defer();
            ECStorageService.currentStore.open().done(function(){
                ECStorageService.currentStore.getKeys('events').done(function(events){
                    $rootScope.$apply(function(){
                        def.resolve( events.length > 0 );
                    });                    
                });
            });            
            return def.promise;
        },
        getLocalData: function(){
            var def = $q.defer();            
            ECStorageService.currentStore.open().done(function(){
                ECStorageService.currentStore.getAll('events').done(function(events){
                    $rootScope.$apply(function(){
                        def.resolve({events: events});
                    });                    
                });
            });            
            return def.promise;
        },
        uploadLocalData: function(){            
            var def = $q.defer();
            this.getLocalData().then(function(localData){                
                var evs = {events: []};
                angular.forEach(localData.events, function(ev){
                    ev.event = ev.id;
                    delete ev.id;
                    evs.events.push(ev);
                });

                $http.post('../api/events', evs).then(function(evResponse){                            
                    def.resolve();
                });                      
            });
            return def.promise;
        }
    };
})

/* Factory to fetch optionSets */
.factory('OptionSetService', function() { 
    return {
        getCode: function(options, key){
            if(options){
                for(var i=0; i<options.length; i++){
                    if( key === options[i].displayName){
                        return options[i].code;
                    }
                }
            }            
            return key;
        },        
        getName: function(options, key){
            if(options){
                for(var i=0; i<options.length; i++){                    
                    if( key === options[i].code){
                        return options[i].displayName;
                    }
                }
            }            
            return key;
        }
    };
})

/* Factory to fetch programs */
.factory('ProgramFactory', function($q, $rootScope, SessionStorageService, ECStorageService, CommonUtils) {  
    
    return {
        getProgramsByOu: function(ou, selectedProgram){
            var roles = SessionStorageService.get('USER_ROLES');
            var userRoles = roles && roles.userCredentials && roles.userCredentials.userRoles ? roles.userCredentials.userRoles : [];
            var def = $q.defer();
            
            ECStorageService.currentStore.open().done(function(){
                ECStorageService.currentStore.getAll('programs').done(function(prs){
                    var programs = [];
                    angular.forEach(prs, function(pr){                            
                        if(pr.organisationUnits.hasOwnProperty( ou.id ) && CommonUtils.userHasValidRole(pr, 'programs', userRoles)){
                            programs.push(pr);
                        }
                    });
                    
                    if(programs.length === 0){
                        selectedProgram = null;
                    }
                    else if(programs.length === 1){
                        selectedProgram = programs[0];
                    } 
                    else{
                        if(selectedProgram){
                            var continueLoop = true;
                            for(var i=0; i<programs.length && continueLoop; i++){
                                if(programs[i].id === selectedProgram.id){                                
                                    selectedProgram = programs[i];
                                    continueLoop = false;
                                }
                            }
                            if(continueLoop){
                                selectedProgram = null;
                            }
                        }
                    }
                    
                    $rootScope.$apply(function(){
                        def.resolve({programs: programs, selectedProgram: selectedProgram});
                    });                      
                });
            });
            
            return def.promise;
        }
    };
})

/* factory for handling program related meta-data */
.factory('MetaDataFactory', function($q, $rootScope, ECStorageService) {  
    
    return {        
        get: function(store, uid){
            
            var def = $q.defer();
            
            ECStorageService.currentStore.open().done(function(){
                ECStorageService.currentStore.get(store, uid).done(function(pv){                    
                    $rootScope.$apply(function(){
                        def.resolve(pv);
                    });
                });
            });                        
            return def.promise;
        },
        getByProgram: function(store, program){
            var def = $q.defer();
            var objs = [];
            
            ECStorageService.currentStore.open().done(function(){
                ECStorageService.currentStore.getAll(store).done(function(data){   
                    angular.forEach(data, function(o){
                        if(o.program.id === program){                            
                            objs.push(o);                               
                        }                        
                    });
                    $rootScope.$apply(function(){
                        def.resolve(objs);
                    });
                });                
            });            
            return def.promise;
        },
        getByIds: function(store, ids){
            var def = $q.defer();
            var objs = [];
            
            ECStorageService.currentStore.open().done(function(){
                ECStorageService.currentStore.getAll(store).done(function(data){   
                    angular.forEach(data, function(o){
                        if(ids.indexOf(o.id) !== -1){                            
                            objs.push(o);                               
                        }                        
                    });
                    $rootScope.$apply(function(){
                        def.resolve(objs);
                    });
                });                
            });            
            return def.promise;
        },
        getAll: function(store){
            var def = $q.defer();            
            ECStorageService.currentStore.open().done(function(){
                ECStorageService.currentStore.getAll(store).done(function(objs){                       
                    $rootScope.$apply(function(){
                        def.resolve(objs);
                    });
                });                
            });            
            return def.promise;
        }
    };        
})

/* factory for handling events */
.factory('DHIS2EventFactory', function($http, $q, ECStorageService, $rootScope) {   
    var internalGetByFilters = function(orgUnit, attributeCategoryUrl, pager, paging, ordering, filterings, format) {
        var url;
           if (format === "csv") {
            	url = '../api/events.csv?' + 'orgUnit=' + orgUnit;
        	} else {
            	url = '../api/events.json?' + 'orgUnit=' + orgUnit;
        	}
            
            if(filterings) {
                angular.forEach(filterings,function(filtering) {
                    url += '&' + filtering.field + '=' + filtering.value;
                });
            }
            
            if(attributeCategoryUrl && !attributeCategoryUrl.default){
                url = url + '&attributeCc=' + attributeCategoryUrl.cc + '&attributeCos=' + attributeCategoryUrl.cp;
            }
                
            if(paging){
                var pgSize = pager.pageSize ? pager.pageSize : 50;
                var pg = pager.page ? pager.page : 1;
                pgSize = pgSize > 1 ? pgSize  : 1;
                pg = pg > 1 ? pg : 1; 
                url = url  + '&pageSize=' + pgSize + '&page=' + pg + '&totalPages=true';
            }
            else{
                url = url  + '&skipPaging=true';
            }
            
            if(ordering && ordering.field){
                url = url  + '&order=' + ordering.field;
                if(ordering.direction) {
                    url = url  + ':' + ordering.direction;
                }
            }
            
            var promise = $http.get( url ).then(function(response){                    
                return response.data;        
            }, function(){     
                var def = $q.defer();
                ECStorageService.currentStore.open().done(function(){
                    ECStorageService.currentStore.getAll('events').done(function(evs){
                        var result = {events: [], pager: {pageSize: '', page: 1, toolBarDisplay: 5, pageCount: 1}};
                        angular.forEach(evs, function(ev){                            
                            if(ev.programStage === programStage && ev.orgUnit === orgUnit){
                                ev.event = ev.id;
                                result.events.push(ev);
                            }
                        }); 
                        $rootScope.$apply(function(){
                            def.resolve( result );
                        });                    
                    });
                });            
                return def.promise;
            });            
            
            return promise;
    };
    
    return {
        getByStage: function(orgUnit, programStage, attributeCategoryUrl, pager, paging, format){
            var filterings = [{field:'programStage',value:programStage}];
            return internalGetByFilters(orgUnit, attributeCategoryUrl, pager, paging, null, filterings, format);
        },  
        getByFilters: function(orgUnit, pager, paging, ordering, filterings){
            return internalGetByFilters(orgUnit, null, pager, paging, ordering, filterings);
        },  
        get: function(eventUid){            
            var promise = $http.get('../api/events/' + eventUid + '.json').then(function(response){               
                return response.data;                
            }, function(){
                var p = dhis2.ec.store.get('events', eventUid).then(function(ev){
                    ev.event = eventUid;
                    return ev;
                });
                return p;
            });            
            return promise;
        },        
        create: function(dhis2Event){
            var promise = $http.post('../api/events.json', dhis2Event).then(function(response){
                return response.data;
            }, function(){            
                dhis2Event.id = dhis2.util.uid();  
                dhis2Event.event = dhis2Event.id;
                dhis2.ec.store.set( 'events', dhis2Event );                
                return {response: {importSummaries: [{status: 'SUCCESS', reference: dhis2Event.id}]}};
            });
            return promise;            
        },        
        delete: function(dhis2Event){
            var promise = $http.delete('../api/events/' + dhis2Event.event).then(function(response){
                return response.data;
            }, function(){
                dhis2.ec.store.remove( 'events', dhis2Event.event );
                return response.data;
            });
            return promise;           
        },    
        update: function(dhis2Event){
            var promise = $http.put('../api/events/' + dhis2Event.event, dhis2Event).then(function(response){              
                return response.data;
            }, function(){
                dhis2.ec.store.remove('events', dhis2Event.event);
                dhis2Event.id = dhis2Event.event;
                dhis2.ec.store.set('events', dhis2Event);
            });
            return promise;
        },        
        updateForSingleValue: function(singleValue, fullValue){        
            var promise = $http.put('../api/events/' + singleValue.event + '/' + singleValue.dataValues[0].dataElement, singleValue ).then(function(response){
                 return response.data;
            }, function(){
                dhis2.ec.store.remove('events', fullValue.event);
                fullValue.id = fullValue.event;
                dhis2.ec.store.set('events', fullValue);
            });
            return promise;
        },
        updateForEventDate: function(dhis2Event, fullEvent){
            var promise = $http.put('../api/events/' + dhis2Event.event + '/updateEventDate', dhis2Event).then(function(response){
                return response.data;         
            }, function(){
                dhis2.ec.store.remove('events', fullEvent.event);
                fullEvent.id = fullEvent.event;
                dhis2.ec.store.set('events', fullEvent);
            });
            return promise;
        }
    };    
});