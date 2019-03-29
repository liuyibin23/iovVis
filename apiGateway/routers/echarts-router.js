const express = require('express');

// Line
const temperature_humidity_statistics_line     = require('./echarts/line/temperature-humidity-statistics.js');
const relative_displacement_statistics_line    = require('./echarts/line/relative-displacement-statistics');
const vibration_monitoring_statistics_line     = require('./echarts/line/vibration-monitoring-statistics');
const crack_monitoring_statistics_line         = require('./echarts/line/crack-monitoring-statistics');
const dip_angle_monitoring_statistics_line     = require('./echarts/line/dip-angle-monitoring-statistics');
const cable_force_monitoring_statistics_line   = require('./echarts/line/cable-force-monitoring-statistics');
const deformation_monitoring_statistics_line   = require('./echarts/line/deformation-monitoring-statistics');
const stress_monitoring_statistics_line        = require('./echarts/line/stress-monitoring-statistics');
const strain_monitoring_statistics_line        = require('./echarts/line/strain-monitoring-statistics');

// Bar
const temperature_humidity_statistics_bar      = require('./echarts/bar/temperature-humidity-statistics.js');
const vehicle_load_monitoring_weight_bar       = require('./echarts/bar/vehicle-load-monitoring-weight');
const vehicle_load_monitoring_speed_bar        = require('./echarts/bar/vehicle-load-monitoring-speed');
const rainfall_monitoring_statistics_bar       = require('./echarts/bar/rainfall-monitoring-statistics');

// Pie
const vehicle_load_monitoring_weight_pie       = require('./echarts/pie/vehicle-load-monitoring-weight');
const vehicle_load_monitoring_speed_pie        = require('./echarts/pie/vehicle-load-monitoring-speed');
const vehicle_load_monitoring_car_type_pie     = require('./echarts/pie/vehicle-load-monitoring-car-type');

// Radar
const wind_speed_and_direction_radar          = require('./echarts/Radar/wind-speed-and-direction');

const node_echarts = require('node-echarts');
const util = require('../util/utils');
const logger = require('../util/logger');
// var multipart = require('connect-multiparty');
// var multipartMiddleware = multipart();

const router = express.Router();
// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//     console.log('echarts Time: ', Date.now())
//     next()
// })


var mapFunc = {
    "折线图":[
        {"name":"温度湿度统计",        "func":temperature_humidity_statistics_line},
        {"name":"相对位移监测数据统计", "func":relative_displacement_statistics_line},
        {"name":"震动监测数据统计",     "func":vibration_monitoring_statistics_line},
        {"name":"裂缝监测数据统计",     "func":crack_monitoring_statistics_line},
        {"name":"倾角自动监测数据统计", "func":dip_angle_monitoring_statistics_line},
        {"name":"索力自动监测数据统计", "func":cable_force_monitoring_statistics_line},
        {"name":"变形自动监测数据统计", "func":deformation_monitoring_statistics_line},
        {"name":"应力监测数据统计",     "func":stress_monitoring_statistics_line},
        {"name":"应变监测数据统计",     "func":strain_monitoring_statistics_line},
    ],
    "柱状图":[
        {"name":"温度湿度统计",           "func":temperature_humidity_statistics_bar},
        {"name":"'温度湿度统计告警",       "func":vehicle_load_monitoring_weight_bar},
        {"name":"车辆荷载自动监测数据车重", "func":vehicle_load_monitoring_speed_bar},
        {"name":"车辆荷载自动监测数据车速", "func":rainfall_monitoring_statistics_bar}
    ],
    "饼图":[
        {"name":"车辆荷载自动监测数据车重", "func":vehicle_load_monitoring_weight_pie},
        {"name":"车辆荷载自动监测数据车速", "func":vehicle_load_monitoring_speed_pie},
        {"name":"车辆荷载自动监测数据车型", "func":vehicle_load_monitoring_car_type_pie}
    ],
    "雷达图":[
        {"name":"风速风向自动监测",         "func":wind_speed_and_direction_radar}
    ]
};


// define the home page route
router.get('/', function (req, res) {
    res.send('Echarts Api home page')
})

// define the about route
router.get('/about', function (req, res) {
    res.send('About Echarts')
})

router.post('/:id', async function (req, res) {
    let assetID = req.params.id;
})
 

function processData(option, params, res){
    if (!option)
    {
        util.responData(404, '访问资源不存在。', res);
        return;  
    }

    let config = {
        width: params.chartWidth   ? params.chartWidth  * 100 : 500, // Image width, type is number.
        height: params.chartHeight ? params.chartHeight * 100 : 400, // Image height, type is number.
        option: option, // Echarts configuration, type is Object.
        //If the path  is not set, return the Buffer of image.
        // path:  '', // Path is filepath of the image which will be created.
        enableAutoDispose: true  //Enable auto-dispose echarts after the image is created.
    }
    let bytes = node_echarts(config);
    if (bytes) {
        let data = 'data:image/png;base64,' + bytes.toString('base64');
        util.responData(200, data, res);
    }
}


function generateChart(req, res){
    let type   = req.params.id;
    let params = req.query;
    logger.log('info', 'type:', type, 'namme:', params);
    let token = req.headers['x-authorization'];

    let funcInfo = mapFunc[type];
    let func = null;
    if (funcInfo){
        for (let i = 0; i < funcInfo.length; i++){
            if (funcInfo[i].name === params.chart_name){
                func = funcInfo[i].func;
                break;
            }
        }

        if (func){
            func.fillData(params, token, res, processData);
        }
        else{
            console.log('Error.');
        }
    }
}

router.get('/:id', async function (req, res) {
    generateChart(req, res);
})

module.exports = router