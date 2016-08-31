/* global trackerCapture, angular */

//conroller for tei report
trackerCapture.controller('TeiReportController',
        function($scope,
                $filter,
                $translate,
                CurrentSelection,
                SessionStorageService,
                DateUtils,
                EventUtils,
                TEIService,
                ProgramStageFactory,
                EnrollmentService) {  
    $scope.showProgramReportDetailsDiv = false;
    $scope.enrollmentsByProgram = [];
    $scope.dashboardReady = false;
    $scope.orgUnitLabel = $translate.instant('org_unit');

    $scope.$on('dashboardWidgets', function(event, args) {
        $scope.showProgramReportDetailsDiv = false;
        var selections = CurrentSelection.get();        
        $scope.selectedOrgUnit = SessionStorageService.get('SELECTED_OU');
        $scope.selectedTei = selections.tei;  
        $scope.selectedEntity = selections.te;
        $scope.selectedProgram = selections.pr;
        $scope.optionSets = selections.optionSets;
        $scope.programs = selections.prs;
        $scope.programNames = selections.prNames;  
        $scope.programStageNames = selections.prStNames;
        $scope.dashboardReady = true;
    
        angular.forEach(selections.enrollments, function(en){            
            $scope.enrollmentsByProgram[en.program] = en;
        });
        
        if( $scope.selectedTei ){            
            $scope.getEvents();
        }
        
    });
    
    $scope.$on('tei-report-widget', function(event, args) {
        $scope.getEvents();        
    });
    
    $scope.getEvents = function(){
        
        $scope.dataFetched = false;
        $scope.dataExists = false;
        
        $scope.report = [];
        angular.forEach($scope.programs, function(pr){
            $scope.report[pr.id] = {};
        });
        
        var eventList = angular.copy( CurrentSelection.getSelectedTeiEvents() );        
        if($scope.selectedProgram && $scope.selectedProgram.id){
            eventList = $filter('filter')(eventList, {program: $scope.selectedProgram.id});
        }
        
        angular.forEach(eventList, function(ev){
            if(ev.program && $scope.report[ev.program] && ev.orgUnit){       
                ev.visited = true;
                ev.dueDate = DateUtils.formatFromApiToUser(ev.dueDate);  
                ev.sortingDate = ev.dueDate;
                ev.name = $scope.programStageNames[ev.programStage].name;
                ev.programName = $scope.programNames[ev.program].name;                    
                if(!$scope.report[ev.program].enrollments){
                    $scope.report[ev.program] = {enrollments: {}};
                }
                ev.statusColor = EventUtils.getEventStatusColor(ev); 

                if(ev.eventDate){
                    ev.eventDate = DateUtils.formatFromApiToUser(ev.eventDate);
                    ev.sortingDate = ev.eventDate;
                }
                else{
                    ev.visited = false;
                }                 

                if(ev.enrollment){
                    if($scope.report[ev.program].enrollments[ev.enrollment]){
                        $scope.report[ev.program].enrollments[ev.enrollment].push(ev);
                    }
                    else{
                        $scope.report[ev.program].enrollments[ev.enrollment]= [ev];
                    }
                }                    
                if(!$scope.dataExists){
                    $scope.dataExists = true;
                }
            }                
        });
        
        $scope.dataFetched = true;
        
    };
    
    $scope.showProgramReportDetails = function(pr){
        
        var selections = CurrentSelection.get();
        $scope.selectedTei = selections.tei;
        
        $scope.showProgramReportDetailsDiv = !$scope.showProgramReportDetailsDiv;
        $scope.selectedProgram = pr;
        $scope.selectedReport = $scope.report[pr.id];
        
        //today as report date
        $scope.today = DateUtils.getToday();

        //process tei attributes, this is to have consistent display so that the tei 
        //contains program attributes whether it has value or not
        TEIService.processAttributes($scope.selectedTei, $scope.selectedProgram, $scope.enrollmentsByProgram[pr.id]).then(function(tei){
            $scope.tei = tei;
        });
        
        //get program stage for the selected program
        //they are needed to assign data element names for event data values
        $scope.stagesById = [];  
        $scope.allowProvidedElsewhereExists = [];
        $scope.prStDes = [];
        
        ProgramStageFactory.getByProgram($scope.selectedProgram).then(function(stages){
            $scope.programStages = stages;
            angular.forEach(stages, function(stage){
                var providedElsewhereExists = false;
                for(var i=0; i<stage.programStageDataElements.length; i++){                
                    if(stage.programStageDataElements[i].allowProvidedElsewhere && !providedElsewhereExists){
                        providedElsewhereExists = true;
                        $scope.allowProvidedElsewhereExists[stage.id] = true;
                    }
                    $scope.prStDes[stage.programStageDataElements[i].dataElement.id] = stage.programStageDataElements[i];
                }

                $scope.stagesById[stage.id] = stage;
            });        
        
            //program reports come grouped in enrollment, process for each enrollment
            $scope.enrollments = [];        
            angular.forEach(Object.keys($scope.selectedReport.enrollments), function(enr){        
                //format report data values
                angular.forEach($scope.selectedReport.enrollments[enr], function(ev){

                    angular.forEach(ev.notes, function(note){
                        note.storedDate = DateUtils.formatToHrsMins(note.storedDate);
                    });

                    if(ev.dataValues){
                        ev = EventUtils.processEvent(ev, $scope.stagesById[ev.programStage], $scope.optionSets, $scope.prStDes);
                    }
                });

                //get enrollment details
                EnrollmentService.get(enr).then(function(enrollment){            
                    angular.forEach(enrollment.notes, function(note){
                        note.storedDate = DateUtils.formatToHrsMins(note.storedDate);
                    });            
                    $scope.enrollments.push(enrollment);               
                });
            });    
        });
    };
    
    $scope.close = function(){
        $scope.showProgramReportDetailsDiv = false;
    };
    
    $scope.print = function(divName){
        $scope.showProgramReportDetailsDiv = false;
        var printContents = document.getElementById(divName).innerHTML;
        var popupWin = window.open('', '_blank', 'fullscreen=1');
        popupWin.document.open();
        popupWin.document.write('<html>\n\
                                        <head>\n\
                                                <link rel="stylesheet" type="text/css" href="../dhis-web-commons/bootstrap/css/bootstrap.min.css" />\n\
                                                <link rel="stylesheet" type="text/css" href="../dhis-web-commons/css/print.css" />\n\
                                                <link rel="stylesheet" type="text/css" href="styles/style.css" />\n\
                                                <link rel="stylesheet" type="text/css" href="styles/print.css" />\n\
                                        </head>\n\
                                        <body onload="window.print()">' + printContents + 
                                '</html>');
        popupWin.document.close();       
    };
});