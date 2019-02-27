const express = require('express');

// Line
const temperature_humidity_statistics_line     = require('./echarts/line/temperature-humidity-statistics.js');
const relative_displacement_statistics_line    = require('./echarts/line/relative-displacement-statistics');
const vibration_monitoring_statistics_line     = require('./echarts/line/vibration-monitoring-statistics');
const crack_monitoring_statistics_line         = require('./echarts/line/crack-monitoring-statistics');
const dip_angle_monitoring_statistics_line     = require('./echarts/line/dip-angle-monitoring-statistics');
const cable_force_monitoring_statistics_line   = require('./echarts/line/cable-force-monitoring-statistics');
const stress_strain_monitoring_statistics_line = require('./echarts/line/stress-strain-monitoring-statistics');

// Bar
const temperature_humidity_statistics_bar      = require('./echarts/bar/temperature-humidity-statistics.js');
const vehicle_load_monitoring_weight_bar       = require('./echarts/bar/vehicle-load-monitoring-weight');
const vehicle_load_monitoring_speed_bar        = require('./echarts/bar/vehicle-load-monitoring-speed');
const rainfall_monitoring_statistics_bar       = require('./echarts/bar/rainfall-monitoring-statistics');

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

    // 折线图绑定
    var chartLineMap = new Map();
    chartLineMap.set('温度湿度统计',        temperature_humidity_statistics_line);
    chartLineMap.set('相对位移监测数据统计', relative_displacement_statistics_line);
    chartLineMap.set('震动监测数据统计',     vibration_monitoring_statistics_line);
    chartLineMap.set('裂缝监测数据统计',     crack_monitoring_statistics_line); 
    chartLineMap.set('倾角自动监测数据统计', dip_angle_monitoring_statistics_line);
    chartLineMap.set('索力自动监测数据统计', cable_force_monitoring_statistics_line);
    chartLineMap.set('应力应变监测数据统计', stress_strain_monitoring_statistics_line);

    // 柱状图绑定
    var chartBarMap = new Map();
    chartBarMap.set('温度湿度统计告警',          temperature_humidity_statistics_bar);
    chartBarMap.set('车辆荷载自动监测数据车重',   vehicle_load_monitoring_weight_bar);
    chartBarMap.set('车辆荷载自动监测数据车速',   vehicle_load_monitoring_speed_bar);
    chartBarMap.set('自动监测数据降雨量',        rainfall_monitoring_statistics_bar);

    let cfg = null;
    if (type == '折线图'){
        cfg = chartLineMap.get(params.chart_name);
    } else if (type == '柱状图') {
        cfg = chartBarMap.get(params.chart_name);
    }

    if (cfg){
        cfg.fillData(params, token, res, processData);
    }
    else{
        console.log('Error.');
    }
}

router.get('/:id', async function (req, res) {
    generateChart(req, res);
})

module.exports = router