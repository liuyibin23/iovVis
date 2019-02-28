const axios = require('axios');
const util = require('../../../util/utils');

let option = {
    title : {
        text: '索力监测数据统计分析',
        x: 'center',
        y:'bottom'
    },
    legend: {
        data:['最大索力', '平均索力', '最小索力'],
        //orient: 'vertical',
        x:'center',
        y:'top',
        //backgroundColor: '#eee',
        //borderColor: 'rgba(178,34,34,0.8)',
        borderWidth: 1
    },
    toolbox: {
        show : false
    },
    calculable : false,
    xAxis : [
        {
            type : 'category',
            boundaryGap : false,
            data : []
        }
    ],
    yAxis : [
       {
            type : 'value',
            axisLabel : {
                formatter: '{value}'
            }
        }
    ],
    series : [
        {
            name:'最大索力',
            type:'line',
            data:[]
        },
      	{
            name:'平均索力',
            type:'line',
            data:[]
        },
      	{
            name:'最小索力',
            type:'line',
            data:[]
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
    let keyValue = 'cableForce';   // 位移
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${keyValue}`
     + `&startTs=${params.startTime}&endTs=${params.endTime}&interval=${interval}&limit=${limit}&agg=${aggList[idx]}`;
    api = encodeURI(api);
    //console.log(api);

    axios.get(api, {
        headers: { "X-Authorization": token }
      }).then(response => {
        retCnt++;
        allData[idx] = response.data.cableForce;

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

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        respHasSend = false;
        retCnt = 0;
        for (var i = 0; i < MAX_DATA; i++){
            dataType = '索力';
            getData(i, dataType, params, token, res, callback);
        }
    }
}
module.exports = chart_area;
