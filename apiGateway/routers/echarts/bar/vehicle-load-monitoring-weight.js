const axios = require('axios');
const util = require('../../../util/utils');
const charCfg = require('../../echarts/chartConfig');

let option = {
    title: {
        text: '车辆荷载自动监测数据车重',
        x: 'center',
        y: 'bottom'
    },
    legend: {
        data: ['车重 > 44T', '24T < 车重 < 44T', '7.5T < 车重 < 24T', '2.5T < 车重 < 7.5T', '车重 < 2.5T'],
        //orient: 'vertical',
        x: 'center',
        y: 'top',
        borderWidth: 1
    },
    toolbox: {
        show: false
    },
    xAxis: [
        {
            type: 'category',
            data: []
        }
    ],
    yAxis: [
        {
            type: 'value',
            axisLabel: {
                formatter: '{value} 辆'
            }
        }
    ],
    series: [
        {
            name: '车重 < 2.5T',
            type: 'bar',
            stack: '总量',
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data: []
        },
        {
            name: '2.5T < 车重 < 7.5T',
            type: 'bar',
            stack: '总量',
            data: []
        },
        {
            name: '7.5T < 车重 < 24T',
            type: 'bar',
            stack: '总量',

            //itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data: []
        },
        {
            name: '24T < 车重 < 44T',
            type: 'bar',
            stack: '总量',
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data: []
        },
        {
            name: '车重 > 44T',
            type: 'bar',
            stack: '总量',
            barCateGoryGap: 20,
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data: []
        }
    ]
};

function getChartData(params, plotCfg, token, res, callback){
    let grounCnt = plotCfg.groupInfo.length;
    let loopCnt = (params.endTime - params.startTime) / plotCfg.interval;
    /*
    // 阈值分组参数
    let ySeries = [
        [0,   2.5],
        [2.5, 7.5],
        [7.5, 24],
        [24,  44],
        [44,  999]
    ];*/
    let ySeries = [];
    for (let i = 0; i < grounCnt; i++) {
        let cfg = plotCfg.groupInfo[i];
        ySeries.push([cfg.min, cfg.max]);
    }

    /*
    // 时间分组参数
    let timeSeries =[
        [1548950556000, 1548950756000],
        [1548950756000, 1548950956000],
        [1548950956000, 1548951156000],
        [1548951156000, 1548951356000],
        [1548951356000, 1548951556000]
    ];
    */
    let timeSeries = [];
    // 按时间段拆分成多个组 多少个柱状
    let startTimeFloat = Number.parseFloat(params.startTime);
    for (let j = 0; j < loopCnt; j++) {        
        let startTime = startTimeFloat + j * plotCfg.interval;
        let endTime   = startTime + plotCfg.interval;
        timeSeries.push([startTime, endTime]);
    }


    let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/counts/timeseries?key=${plotCfg.keys}&tsIntervals=${JSON.stringify(timeSeries)}&valueIntervals=${JSON.stringify(ySeries)}&dataType=LONG`;
    api = encodeURI(api);

    axios.get(api, {
        headers: { "X-Authorization": token }
    }).then(resp => {
        let data = resp.data;

        // 分组处理
        for (let i = 0; i < loopCnt; i++) {
            option.xAxis[0].data.push(i + 1);
        }

        for (let i = 0; i < data.length; i++){
            let col_data = data[i];

            for (let j = 0; j < col_data.length; j++){
                option.series[j].data[i] = col_data[j];
            }
        }
        
        callback(option, params, res);
    }).catch(err =>{
        console.log(err);
        callback(option, params, res);
    });
}

function resetPreData() {
    respHasSend = false;
    retCnt = 0;
    option.xAxis[0].data = [];
    let len = option.series.length;
    for (let idx = 0; idx < len; idx++) {
        option.series[idx].data = [];
    }
}

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        resetPreData();
        
        var plotCfg = charCfg.getCfgParams(params.chart_name, 'Bar');

        getChartData(params, plotCfg, token, res, callback);
    }
}
module.exports = chart_area;
