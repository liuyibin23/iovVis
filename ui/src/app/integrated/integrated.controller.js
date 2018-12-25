/*
 * Copyright © 2016-2018 The BeiDouApp Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* eslint-disable */
import './integrated.scss';
import Subscription from '../api/subscription';

import Cesium from 'cesium/Cesium';
import "cesium/Widgets/widgets.css";
import "cesium/Widgets/lighter.css";
/*@ngInject*/
export default function IntegratedController(
    $scope, $filter, $mdMedia, $q, $document, menu, $state, $timeout, 
    $log, tbRaf, types, utils, timeService,
    datasourceService, alarmService,  dashboardService, deviceService
    ) {
       
    var vm = this;

    vm.sectionColspan = sectionColspan;
    vm.openDashboard = openDashboard;
    // $rootScope.forceFullscreen = !$rootScope.forceFullscreen;
    // $scope.searchConfig.searchEnabled = true;
    vm.isLibraryOpen = true;



    // please search IN Google： cesium display a echart on cesium 
// A simple demo of 3D Tiles feature picking with hover and select behavior
// Building data courtesy of NYC OpenData portal: http://www1.nyc.gov/site/doitt/initiatives/3d-building.page
// var viewer = new Cesium.Viewer('cesiumContainer', {
//     terrainProvider: Cesium.createWorldTerrain(),
//     imageryProvider:new Cesium.createOpenStreetMapImageryProvider({
//         url : 'https://a.tile.openstreetmap.org/'
//     }),
//     animation: false,
//     timeline: false
// });
var viewer = new Cesium.Viewer('cesiumContainer', {
    terrainProvider: Cesium.createWorldTerrain(),
    imageryProvider: new Cesium.UrlTemplateImageryProvider({
        url : 'http://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
        credit : 'Map tiles by CartoDB, under CC BY 3.0. Data by OpenStreetMap, under ODbL.'
    }),
    animation: false,
    timeline: false
});

viewer.scene.globe.depthTestAgainstTerrain = true;

// Set the initial camera view to look at Manhattan
var initialPosition = Cesium.Cartesian3.fromDegrees(-74.01881302800248, 40.69114333714821, 753);
var initialOrientation = new Cesium.HeadingPitchRoll.fromDegrees(21.27879878293835, -21.34390550872461, 0.0716951918898415);
viewer.scene.camera.setView({
    destination: initialPosition,
    orientation: initialOrientation,
    endTransform: Cesium.Matrix4.IDENTITY
});

// Load the NYC buildings tileset
var tileset = new Cesium.Cesium3DTileset({ url: Cesium.IonResource.fromAssetId(5741) });
viewer.scene.primitives.add(tileset);

// HTML overlay for showing feature name on mouseover
// var nameOverlay = angular.element(document).createElement('div');
// viewer.container.appendChild(nameOverlay);
// nameOverlay.className = 'backdrop';
// nameOverlay.style.display = 'none';
// nameOverlay.style.position = 'absolute';
// nameOverlay.style.bottom = '0';
// nameOverlay.style.left = '0';
// nameOverlay.style['pointer-events'] = 'none';
// nameOverlay.style.padding = '4px';
// nameOverlay.style.backgroundColor = 'black';

// Information about the currently selected feature
var selected = {
    feature: undefined,
    originalColor: new Cesium.Color()
};

// An entity object which will hold info about the currently selected feature for infobox display
var selectedEntity = new Cesium.Entity();

// Get default left click handler for when a feature is not picked on left click
var clickHandler = viewer.screenSpaceEventHandler.getInputAction(Cesium.ScreenSpaceEventType.LEFT_CLICK);

// If silhouettes are supported, silhouette features in blue on mouse over and silhouette green on mouse click.
// If silhouettes are not supported, change the feature color to yellow on mouse over and green on mouse click.
if (Cesium.PostProcessStageLibrary.isSilhouetteSupported(viewer.scene)) {
    // Silhouettes are supported
    var silhouetteBlue = Cesium.PostProcessStageLibrary.createEdgeDetectionStage();
    silhouetteBlue.uniforms.color = Cesium.Color.BLUE;
    silhouetteBlue.uniforms.length = 0.01;
    silhouetteBlue.selected = [];

    var silhouetteGreen = Cesium.PostProcessStageLibrary.createEdgeDetectionStage();
    silhouetteGreen.uniforms.color = Cesium.Color.LIME;
    silhouetteGreen.uniforms.length = 0.01;
    silhouetteGreen.selected = [];

    viewer.scene.postProcessStages.add(Cesium.PostProcessStageLibrary.createSilhouetteStage([silhouetteBlue, silhouetteGreen]));

    // Silhouette a feature blue on hover.
    viewer.screenSpaceEventHandler.setInputAction(function onMouseMove(movement) {
        // If a feature was previously highlighted, undo the highlight
        silhouetteBlue.selected = [];

        // Pick a new feature
        var pickedFeature = viewer.scene.pick(movement.endPosition);
        if (!Cesium.defined(pickedFeature)) {
            // nameOverlay.style.display = 'none';
            return;
        }

        // A feature was picked, so show it's overlay content
        // nameOverlay.style.display = 'block';
        // nameOverlay.style.bottom = viewer.canvas.clientHeight - movement.endPosition.y + 'px';
        // nameOverlay.style.left = movement.endPosition.x + 'px';
        var name = pickedFeature.getProperty('name');
        if (!Cesium.defined(name)) {
            name = pickedFeature.getProperty('id');
        }
        // nameOverlay.textContent = name;

        // Highlight the feature if it's not already selected.
        if (pickedFeature !== selected.feature) {
            silhouetteBlue.selected = [pickedFeature];
        }
    }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);

    // Silhouette a feature on selection and show metadata in the InfoBox.
    viewer.screenSpaceEventHandler.setInputAction(function onLeftClick(movement) {
        // If a feature was previously selected, undo the highlight
        silhouetteGreen.selected = [];

        // Pick a new feature
        var pickedFeature = viewer.scene.pick(movement.position);
        if (!Cesium.defined(pickedFeature)) {
            clickHandler(movement);
            return;
        }

        // Select the feature if it's not already selected
        if (silhouetteGreen.selected[0] === pickedFeature) {
            return;
        }

        // Save the selected feature's original color
        var highlightedFeature = silhouetteBlue.selected[0];
        if (pickedFeature === highlightedFeature) {
            silhouetteBlue.selected = [];
        }

        // Highlight newly selected feature
        silhouetteGreen.selected = [pickedFeature];

        // Set feature infobox description
        var featureName = pickedFeature.getProperty('name');
        selectedEntity.name = '基础设施' + featureName;
        selectedEntity.description = 'Loading <div class="cesium-infoBox-loading"></div>';
        viewer.selectedEntity = selectedEntity;
        selectedEntity.description = '<table class="cesium-infoBox-defaultTable"><tbody>' +
                                     '<tr><th>编号</th><td>' + pickedFeature.getProperty('BIN') + '</td></tr>' +
                                     '<tr><th>BIM名称</th><td>' + pickedFeature.getProperty('DOITT_ID') + '</td></tr>' +
                                     '<tr><th>数据测点</th><td>' + pickedFeature.getProperty('SOURCE_ID') + '</td></tr>' +
                                     '</tbody></table>';
    }, Cesium.ScreenSpaceEventType.LEFT_CLICK);
} else {
    // Silhouettes are not supported. Instead, change the feature color.

    // Information about the currently highlighted feature
    var highlighted = {
        feature : undefined,
        originalColor : new Cesium.Color()
    };

    // Color a feature yellow on hover.
    viewer.screenSpaceEventHandler.setInputAction(function onMouseMove(movement) {
        // If a feature was previously highlighted, undo the highlight
        if (Cesium.defined(highlighted.feature)) {
            highlighted.feature.color = highlighted.originalColor;
            highlighted.feature = undefined;
        }
        // Pick a new feature
        var pickedFeature = viewer.scene.pick(movement.endPosition);
        if (!Cesium.defined(pickedFeature)) {
            // nameOverlay.style.display = 'none';
            return;
        }
        // A feature was picked, so show it's overlay content
        // nameOverlay.style.display = 'block';
        // nameOverlay.style.bottom = viewer.canvas.clientHeight - movement.endPosition.y + 'px';
        // nameOverlay.style.left = movement.endPosition.x + 'px';
        var name = pickedFeature.getProperty('name');
        if (!Cesium.defined(name)) {
            name = pickedFeature.getProperty('id');
        }
        // nameOverlay.textContent = name;
        // Highlight the feature if it's not already selected.
        if (pickedFeature !== selected.feature) {
            highlighted.feature = pickedFeature;
            Cesium.Color.clone(pickedFeature.color, highlighted.originalColor);
            pickedFeature.color = Cesium.Color.YELLOW;
        }
    }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);

    // Color a feature on selection and show metadata in the InfoBox.
    viewer.screenSpaceEventHandler.setInputAction(function onLeftClick(movement) {
        // If a feature was previously selected, undo the highlight
        if (Cesium.defined(selected.feature)) {
            selected.feature.color = selected.originalColor;
            selected.feature = undefined;
        }
        // Pick a new feature
        var pickedFeature = viewer.scene.pick(movement.position);
        if (!Cesium.defined(pickedFeature)) {
            clickHandler(movement);
            return;
        }
        // Select the feature if it's not already selected
        if (selected.feature === pickedFeature) {
            return;
        }
        selected.feature = pickedFeature;
        // Save the selected feature's original color
        if (pickedFeature === highlighted.feature) {
            Cesium.Color.clone(highlighted.originalColor, selected.originalColor);
            highlighted.feature = undefined;
        } else {
            Cesium.Color.clone(pickedFeature.color, selected.originalColor);
        }
        // Highlight newly selected feature
        pickedFeature.color = Cesium.Color.LIME;
        // Set feature infobox description
        var featureName = pickedFeature.getProperty('name');
        selectedEntity.name = featureName;
        selectedEntity.description = 'Loading <div class="cesium-infoBox-loading"></div>';
        viewer.selectedEntity = selectedEntity;
        selectedEntity.description = '<table class="cesium-infoBox-defaultTable"><tbody>' +
                                     '<tr><th>BIN</th><td>' + pickedFeature.getProperty('BIN') + '</td></tr>' +
                                     '<tr><th>DOITT ID</th><td>' + pickedFeature.getProperty('DOITT_ID') + '</td></tr>' +
                                     '<tr><th>SOURCE ID</th><td>' + pickedFeature.getProperty('SOURCE_ID') + '</td></tr>' +
                                     '<tr><th>Longitude</th><td>' + pickedFeature.getProperty('longitude') + '</td></tr>' +
                                     '<tr><th>Latitude</th><td>' + pickedFeature.getProperty('latitude') + '</td></tr>' +
                                     '<tr><th>Height</th><td>' + pickedFeature.getProperty('height') + '</td></tr>' +
                                     '<tr><th>Terrain Height (Ellipsoid)</th><td>' + pickedFeature.getProperty('TerrainHeight') + '</td></tr>' +
                                     '</tbody></table>';
    }, Cesium.ScreenSpaceEventType.LEFT_CLICK);
}

    // var viewer = new Cesium.Viewer('cesiumContainer', {
    //     terrainProvider: Cesium.createWorldTerrain(),
    //     imageryProvider:new Cesium.createOpenStreetMapImageryProvider({
    //         url : 'https://a.tile.openstreetmap.org/'
    //     }),
    //     animation: false,
    //     timeline: false
    // });
    
    // viewer.scene.globe.depthTestAgainstTerrain = true;
    
    // // Set the initial camera view to look at Manhattan
    // var initialPosition = Cesium.Cartesian3.fromDegrees(-74.01881302800248, 40.69114333714821, 753);
    // var initialOrientation = new Cesium.HeadingPitchRoll.fromDegrees(21.27879878293835, -21.34390550872461, 0.0716951918898415);
    // viewer.scene.camera.setView({
    //     destination: initialPosition,
    //     orientation: initialOrientation,
    //     endTransform: Cesium.Matrix4.IDENTITY
    // });
    
    // // Load the NYC buildings tileset.
    // var tileset = new Cesium.Cesium3DTileset({ url: Cesium.IonResource.fromAssetId(5741) });
    // viewer.scene.primitives.add(tileset);
    
    // // Color buildings based on their height.
    // function colorByHeight() {
    //     tileset.style = new Cesium.Cesium3DTileStyle({
    //         color: {
    //             conditions: [
    //                 ['${height} >= 300', 'rgba(45, 0, 75, 0.5)'],
    //                 ['${height} >= 200', 'rgb(102, 71, 151)'],
    //                 ['${height} >= 100', 'rgb(170, 162, 204)'],
    //                 ['${height} >= 50', 'rgb(224, 226, 238)'],
    //                 ['${height} >= 25', 'rgb(252, 230, 200)'],
    //                 ['${height} >= 10', 'rgb(248, 176, 87)'],
    //                 ['${height} >= 5', 'rgb(198, 106, 11)'],
    //                 ['true', 'rgb(127, 59, 8)']
    //             ]
    //         }
    //     });
    // }
    
    // // Color buildings by their latitude coordinate.
    // function colorByLatitude() {
    //     tileset.style = new Cesium.Cesium3DTileStyle({
    //         defines: {
    //             latitudeRadians: 'radians(${latitude})'
    //         },
    //         color: {
    //             conditions: [
    //                 ['${latitudeRadians} >= 0.7125', "color('purple')"],
    //                 ['${latitudeRadians} >= 0.712', "color('red')"],
    //                 ['${latitudeRadians} >= 0.7115', "color('orange')"],
    //                 ['${latitudeRadians} >= 0.711', "color('yellow')"],
    //                 ['${latitudeRadians} >= 0.7105', "color('lime')"],
    //                 ['${latitudeRadians} >= 0.710', "color('cyan')"],
    //                 ['true', "color('blue')"]
    //             ]
    //         }
    //     });
    // }
    
    // // Color buildings by distance from a landmark.
    // function colorByDistance() {
    //     tileset.style = new Cesium.Cesium3DTileStyle({
    //         defines : {
    //             distance : 'distance(vec2(radians(${longitude}), radians(${latitude})), vec2(-1.291777521, 0.7105706624))'
    //         },
    //         color : {
    //             conditions : [
    //                 ['${distance} > 0.0012',"color('gray')"],
    //                 ['${distance} > 0.0008', "mix(color('yellow'), color('red'), (${distance} - 0.008) / 0.0004)"],
    //                 ['${distance} > 0.0004', "mix(color('green'), color('yellow'), (${distance} - 0.0004) / 0.0004)"],
    //                 ['${distance} < 0.00001', "color('white')"],
    //                 ['true', "mix(color('blue'), color('green'), ${distance} / 0.0004)"]
    //             ]
    //         }
    //     });
    // }
    
    // // Color buildings with a '3' in their name.
    // function colorByNameRegex() {
    //     tileset.style = new Cesium.Cesium3DTileStyle({
    //         color : "(regExp('3').test(String(${name}))) ? color('cyan', 0.9) : color('purple', 0.1)"
    //     });
    // }
    
    // // Show only buildings greater than 200 meters in height.
    // function hideByHeight() {
    //     tileset.style = new Cesium.Cesium3DTileStyle({
    //         show : '${height} > 200'
    //     });
    // }
    
    // Sandcastle.addToolbarMenu([{
    //     text : 'Color By Height',
    //     onselect : function() {
    //         colorByHeight();
    //     }
    // }, {
    //     text : 'Color By Latitude',
    //     onselect : function() {
    //         colorByLatitude();
    //     }
    // }, {
    //     text : 'Color By Distance',
    //     onselect : function() {
    //         colorByDistance();
    //     }
    // }, {
    //     text : 'Color By Name Regex',
    //     onselect : function() {
    //         colorByNameRegex();
    //     }
    // }, {
    //     text : 'Hide By Height',
    //     onselect : function() {
    //         hideByHeight();
    //     }
    // }]);
    
    // colorByHeight();
    
    
    


    // var viewer = new Cesium.Viewer('cesiumContainer',{
    //     imageryProvider:new Cesium.createOpenStreetMapImageryProvider({
    //         url : 'https://a.tile.openstreetmap.org/'
    //     }),
    //     animation: false,
    //     timeline: false
    // });

    // // viewer.imageryLayers.remove(viewer.imageryLayers.get(0));
    // // var osm = Cesium.createOpenStreetMapImageryProvider({
    // //     url : 'https://a.tile.openstreetmap.org/'
    // // });
    // // viewer.imageryLayer.addImageryProvider(osm);

	// var tileset = viewer.scene.primitives.add(new Cesium.Cesium3DTileset({
    //     // url : 'http://localhost:3000/3dtiles/tilesets/TilesetWithDiscreteLOD/tileset.json'
    //     url : '3dtiles/tilesets/TilesetWithDiscreteLOD/tileset.json'
    // }));

    // viewer.zoomTo(tileset, new Cesium.HeadingPitchRange(0, -0.5, 0));

    // var homeCameraView;
    // tileset.readyPromise.then(function (argument) {
    //     var x = tileset._root._initialTransform[12];
    //     var y = tileset._root._initialTransform[13];
    //     var z = tileset._root._initialTransform[14];
    //     var position = new Cesium.Cartesian3(x,y,z);
    //     //The x axis points in the local east direction.
    //     //The y axis points in the local north direction.
    //     //The z axis points in the direction of the ellipsoid surface normal which passes through the position.
    //     var mat = Cesium.Transforms.eastNorthUpToFixedFrame(position);//旋转模型，使其在给定点，x轴指向正东，y轴指向正北，z轴指向通过该位置的椭球面法线方向
    //     // var heading = 90;//绕z轴旋转90度，只是示例可以旋转，无其他用意
    //     // var rotationX = Cesium.Matrix4.fromRotationTranslation(Cesium.Matrix3.fromRotationZ(Cesium.Math.toRadians(heading)));
    //     // Cesium.Matrix4.multiply(mat,rotationX,mat);
    //     tileset._root.transform = mat;

    //     // viewer.homeButton.viewModel.command.beforeExecute.addEventListener(function(e){
    //     //     e.cancel = true;
    //     //     viewer.camera.flyToBoundingSphere(tileset.boundingSphere);
    //     // });
    // });

    // viewer.homeButton.viewModel.command.beforeExecute.addEventListener(function(e){
    //     e.cancel = true;
    //     var west = 103.83698065617835;
    //     var south = 30.549742872263604;
    //     var east = 104.25786771989854;
    //     var north = 30.829369938500545;

    //     var rectangle = Cesium.Rectangle.fromDegrees(west, south, east, north);

    //     viewer.camera.flyTo({
    //         destination : rectangle,
    //         // orientation:{
    //         //     heading:Cesium.Math.toRadians(175.0),
    //         //     pitch:Cesium.Math.toRadians(-35.0),
    //         //     roll:0.0
    //         // }
    //     });

    //     // 显示区域矩形
    //     viewer.entities.add({
    //         rectangle : {
    //             coordinates : rectangle,
    //             fill : false,
    //             outline : true,
    //             outlineColor : Cesium.Color.RED
    //         }
    //     });
    // });

    // $scope.$watch(function () { return $mdMedia('gt-lg'); }, function () {
    //     updateColumnCount();
    // });

    // updateColumnCount();

    // vm.model = menu.getHomeSections();

    // /*订阅开始*/
    // var timeWindowConfig = {
    //     fixedWindow: null,
    //     realtimeWindowMs: null,
    //     aggregation: {
    //         interval: 10000,
    //         limit: 2000,
    //         type: types.aggregation.avg.value
    //     },
    //     realtime: {
    //         interval: 10000,         //间隔
    //         timewindowMs: 10000,    //持续
    //     }
    // };
    // var stDiff = 10;
    // var timeWindow = timeService.createSubscriptionTimewindow(timeWindowConfig, stDiff);
    // var datasourceDemo = {
    //     "type":"entity",
    //     "dataKeys":[
    //         {
    //             "name":"temperature",
    //             "type":"timeseries",
    //             "label":"temperature",
    //             "color":"#2196f3",
    //             "settings":{},
    //             "_hash":0.39904519210542744,
    //             "pattern":"temperature",
    //             "hidden":false
    //         }
    //     ],
    //     "entityAliasId":"dbe3b0de-69d8-5f93-80c3-cd513a89209d",
    //     "aliasName":"A桥巡检",
    //     "entity":{
    //         "id":{"entityType":"DEVICE","id":"056a2f60-e31a-11e8-be95-f3713e6700c3"},
    //         "createdTime":1541656153430,
    //         "additionalInfo":null,
    //         "tenantId":{"entityType":"TENANT","id":"5d92c2e0-dd0e-11e8-be4d-9104f478b9f9"},
    //         "customerId":{"entityType":"CUSTOMER","id":"13814000-1dd2-11b2-8080-808080808080"},
    //         "name":"A桥巡检员","type":"巡检员"
    //     },
    //     "entityId":"056a2f60-e31a-11e8-be95-f3713e6700c3",
    //     "entityType":"DEVICE",
    //     "entityName":"A桥巡检员",
    //     "name":"A桥巡检员",
    //     "entityDescription":""
    // };

    // var listener = {
    //     subscriptionType: 'timeseries',     //types.widgetType.timeseries.value, 订阅者（widget）的类型
    //     subscriptionTimewindow: timeWindow, //时间查询窗口
    //     entityType: 'DEVICE',               //设备
    //     entityId: '056a2f60-e31a-11e8-be95-f3713e6700c3', //A桥巡检员
    //     datasource: datasourceDemo,
    //     datasourceIndex: 0,
    //     dataUpdated: function (data, datasourceIndex, dataKeyIndex, apply) {
    //         $log.log('AWEN-->')
    //         onDataUpdated(data, datasourceIndex, dataKeyIndex, apply);
    //     }
    //     /*
    //     updateRealtimeSubscription: function () {
    //         this.subscriptionTimewindow = subscription.updateRealtimeSubscription();
    //         return this.subscriptionTimewindow;
    //     },
    //     setRealtimeSubscription: function (subscriptionTimewindow) {
    //         subscription.updateRealtimeSubscription(angular.copy(subscriptionTimewindow));
    //     }
    //     */
    // };
    // datasourceService.subscribeToDatasource(listener);
    
    // function onDataUpdated(sourceData, datasourceIndex, dataKeyIndex, apply) {
    //     // $log.info("AWEN-->websocket data callback",sourceData,datasourceIndex,dataKeyIndex);
    // }
    /*订阅结束*/

    // dashboard id---> d96c1d40-de3e-11e8-90b2-6708b10d2f5b 战略情报
    // dashboard id---> af5dadb0-e5ba-11e8-be95-f3713e6700c3 桥梁数据展示
    function openDashboard(dashboardID) {
        $state.go('home.integrated.dashboard', { dashboardId: dashboardID });
    }

    function updateColumnCount() {
        vm.cols = 2;
        if ($mdMedia('lg')) {
            vm.cols = 3;
        }
        if ($mdMedia('gt-lg')) {
            vm.cols = 4;
        }
    }

    function sectionColspan(section) {
        var colspan = vm.cols;
        if (section && section.places && section.places.length <= colspan) {
            colspan = section.places.length;
        }
        return colspan;
    }
}
