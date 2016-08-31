/* global trackerCapture, angular */

trackerCapture.controller('RelationshipController',
        function($scope,
                $rootScope,
                $modal,                
                $location,
                TEIService,
                AttributesFactory,
                CurrentSelection,
                RelationshipFactory,
                ModalService,
                DialogService) {
    $scope.dashboardReady = false;
    $rootScope.showAddRelationshipDiv = false;    
    $scope.relatedProgramRelationship = false;
    
    //listen for the selected entity       
    $scope.$on('dashboardWidgets', function(event, args) { 
        $scope.relationshipTypes = []; 
        $scope.relationships = [];
        $scope.relatedTeis = [];
        $scope.selections = CurrentSelection.get();
        $scope.optionSets = $scope.selections.optionSets;
        $scope.selectedTei = angular.copy($scope.selections.tei);        
        $scope.attributesById = CurrentSelection.getAttributesById();
        
        $scope.attributes = [];
        for(var key in $scope.attributesById){
            if($scope.attributesById.hasOwnProperty(key)){
                $scope.attributes.push($scope.attributesById[key]);
            }            
        }
        
        $scope.trackedEntity = $scope.selections.te;
        $scope.selectedEnrollment = $scope.selections.selectedEnrollment;
        $scope.selectedProgram = $scope.selections.pr;
        $scope.programs = $scope.selections.pr;
        
        RelationshipFactory.getAll().then(function(rels){
            $scope.relationshipTypes = rels;    
            angular.forEach(rels, function(rel){
                $scope.relationships[rel.id] = rel;
            });
            
            $scope.dashboardReady = true;
            setRelationships();            
        });
    });
    
    $scope.showAddRelationship = function(related) {
        $scope.relatedProgramRelationship = related;
        $rootScope.showAddRelationshipDiv = !$rootScope.showAddRelationshipDiv;
       
        if($rootScope.showAddRelationshipDiv){
            var modalInstance = $modal.open({
                templateUrl: 'components/teiadd/tei-add.html',
                controller: 'TEIAddController',
                windowClass: 'modal-full-window',
                resolve: {
                    relationshipTypes: function () {
                        return $scope.relationshipTypes;
                    },
                    selectedAttribute: function(){
                        return null;
                    },
                    existingAssociateUid: function(){
                        return null;
                    },
                    addingRelationship: function(){
                        return true;
                    },
                    selections: function () {
                        return $scope.selections;
                    },
                    selectedTei: function(){
                        return $scope.selectedTei;
                    },
                    selectedProgram: function(){
                        return $scope.selectedProgram;
                    },
                    relatedProgramRelationship: function(){
                        return $scope.relatedProgramRelationship;
                    }
                }
            });

            modalInstance.result.then(function (relationships) {
                $scope.selectedTei.relationships = relationships;                
                setRelationships();
            });
        }
    };
    
    $scope.removeRelationship = function(rel){
        
        var modalOptions = {
            closeButtonText: 'cancel',
            actionButtonText: 'delete',
            headerText: 'delete',
            bodyText: 'are_you_sure_to_delete_relationship'
        };

        ModalService.showModal({}, modalOptions).then(function(result){
            
            var index = -1;
            for(var i=0; i<$scope.selectedTei.relationships.length; i++){
                if($scope.selectedTei.relationships[i].relationship === rel.relId){
                    index = i;
                    break;
                }
            }

            if( index !== -1 ){
                $scope.selectedTei.relationships.splice(index,1);
                var trimmedTei = angular.copy($scope.selectedTei);
                angular.forEach(trimmedTei.relationships, function(rel){
                    delete rel.relative;
                });
                TEIService.update(trimmedTei, $scope.optionSets, $scope.attributesById).then(function(response){
                    if(response.response && response.response.status !== 'SUCCESS'){//update has failed
                        var dialogOptions = {
                                headerText: 'update_error',
                                bodyText: response.message
                            };
                        DialogService.showDialog({}, dialogOptions);
                        return;
                    }                    
                    setRelationships();
                });
            }
        });        
    };
    
    $scope.showDashboard = function(teiId, relId){
        
        var dashboardProgram = null;
        
        if($scope.selectedProgram && $scope.selectedProgram.relationshipType){
            if($scope.selectedProgram.relationshipType.id === relId && $scope.selectedProgram.relatedProgram ){
                dashboardProgram = $scope.selectedProgram.relatedProgram.id;
            }
        }        
    
        $location.path('/dashboard').search({tei: teiId, program: dashboardProgram}); 
    };
    
    var setRelationships = function(){
        $scope.relatedTeis = [];
        angular.forEach($scope.selectedTei.relationships, function(rel){
            var teiId = rel.trackedEntityInstanceA;
            var relName = $scope.relationships[rel.relationship].aIsToB;
            if($scope.selectedTei.trackedEntityInstance === rel.trackedEntityInstanceA){
                teiId = rel.trackedEntityInstanceB;
                relName = $scope.relationships[rel.relationship].bIsToA;
            }
            var relative = {trackedEntityInstance: teiId, relName: relName, relId: rel.relationship, attributes: getRelativeAttributes(rel)};            
            $scope.relatedTeis.push(relative);
        });
        
        var selections = CurrentSelection.get();
        CurrentSelection.set({tei: $scope.selectedTei, te: $scope.selectedTei.trackedEntity, prs: selections.prs, pr: $scope.selectedProgram, prNames: selections.prNames, prStNames: selections.prStNames, enrollments: selections.enrollments, selectedEnrollment: $scope.selectedEnrollment, optionSets: selections.optionSets});       
    };
    
    var getRelativeAttributes = function(tei){
        
        var attributes = {};
        
        if(tei && tei.relative && tei.relative.attributes && !tei.relative.processed){
            angular.forEach(tei.relative.attributes, function(att){                
                var val = AttributesFactory.formatAttributeValue(att,$scope.attributesById, $scope.optionSets, 'USER');                
                attributes[att.attribute] = val;
            });
        }
        
        if(tei && tei.relative && tei.relative.processed){
            attributes = tei.relative.attributes;
        }
        
        return attributes;
    };
});