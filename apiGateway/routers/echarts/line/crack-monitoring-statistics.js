const axios = require('axios');
const util = require('../../../util/utils');
const WebSocket = require('ws');

let option = {
    title : {
        text: '裂缝监测数据统计分析',
        x: 'center',
        y:'bottom'
    },
    legend: {
        data:['最大裂缝', '平均裂缝', '最小裂缝'],
        orient: 'vertical',
        x:'80%',
        y:'10%',
        backgroundColor: '#eee',
        borderColor: 'rgba(178,34,34,0.8)',
        borderWidth: 2
    },
    toolbox: {
        show : false
    },
    calculable : false,
    xAxis : [
        {
            type : 'category',
            boundaryGap : false,
            data : [
                //1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
            ]
        }
    ],
    yAxis : [
       {
            type : 'value',
            axisLabel : {
                formatter: '{value} mm'
            }
        }
    ],
    series : [
        {
            name:'最大裂缝',
            type:'line',
            data:[
                //80,20,40,10,30,40,70,80,3, 4, 50,60,80,90,20,10, 10,60,80,90,40,70,80,3, 4, 50,60,80,90,20,10, 
            ]
        },
      	{
            name:'平均裂缝',
            type:'line',
            data:[
                //10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40,1,20,30,40,10,20,30,40,10,20
            ]
        },
      	{
            name:'最小裂缝',
            type:'line',
            data:[
               // 1,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8,2,3,6,7,8,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8
            ]
        }
    ]
};

var allData = [];
var MAX_DATA = 3;
var aggList = ['MAX', 'AVG', 'MIN'];
var retCnt = 0;
var respHasSend = 0;

function processData(res, params, callback){
    if (allData[0]){
        for (let i = 0; i < allData[0].length; i++) {                    
            for (let idx = 0; idx < MAX_DATA; idx++) {
                if (allData[idx] && allData[idx][i]) {
                    val = Number.parseFloat(allData[idx][i].value);
                    option.series[idx].data.push(val);
                }
            }

            option.xAxis[0].data.push(i);
        }
    }

    callback(option, params, res);
}

async function getData(idx, dataType, params, token, res, callback){
    let interval = 10 * 1000;
    let limit    = 1000;
    let keyValue = 'crackWidth';   // 裂缝宽度
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${keyValue}`
     + `&startTs=${params.startTime}&endTs=${params.endTime}&interval=${interval}&limit=${limit}&agg=${aggList[idx]}`;
    api = encodeURI(api);
    //console.log(api);

    axios.get(api, {
        headers: { "X-Authorization": token }
      }).then(response => {
        retCnt++;
        allData[idx] = response.data.crackWidth;

        if (retCnt == MAX_DATA){
            console.log('all data receive');
            processData(res, params, callback);
        }
      }).catch(err => {
        if (!respHasSend) {
            respHasSend = true;
            //util.responErrorMsg(err, res);
            processData(res, params, callback);
        }
      });   
}

function resetPreData(){
    respHasSend = false;
    retCnt = 0;
    option.xAxis[0].data = [];
    for (let idx = 0; idx < MAX_DATA; idx++) {
        option.series[idx].data = [];
    }
}

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        resetPreData();
        for (var i = 0; i < MAX_DATA; i++){
            dataType = 'crackWidth';
            getData(i, dataType, params, token, res, callback);
        }
    }
}
module.exports = chart_area;
