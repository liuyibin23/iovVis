const axios = require('axios');
const util = require('../../../util/utils');

let option = {
    title : {
        text: '振动监测数据统计',
        x: 'center',
        y:'bottom'
    },
    legend: {
        data:['加速度'],
        //orient: 'vertical',
        x:'center',
        y:'top',
        //ackgroundColor: '#eee',
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
            axisLabel:{
                interval:30,    
          },
            data : []
        }
    ],
    yAxis : [
        {
            type : 'value',
            axisLabel : {
                formatter: '{value} m/s^2'
            }
        }
    ],
    series : [
        {
            name:'加速度',
            type:'line',
          	data:[]
        }
    ]
};

var allData = [];
var MAX_DATA = 1;
var retCnt = 0;
var respHasSend = 0;

function processData(res, params, callback){
    if (allData){
        for (let i = 0; i < allData.length; i++) {                    
            val = Number.parseFloat(allData[i].value);
            option.series[0].data.push(val);

            option.xAxis[0].data.push(i);
        }

        if (allData.length > 100) {
            option.xAxis[0].axisLabel.interval = Math.ceil(allData.length / 30);
        }
    }

    callback(option, params, res);
}

async function getData(params, token, res, callback){
    let keyValue = '加速度_avg';   // 震动
    let limit    =  Math.ceil((params.endTime - params.startTime) / 1000);

    let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${keyValue}`
     + `&startTs=${params.startTime}&endTs=${params.endTime}&limit=${limit}`;
    api = encodeURI(api);
    //console.log(api);

    axios.get(api, {
        headers: { "X-Authorization": token }
      }).then(response => {
        retCnt++;
        allData = response.data[keyValue];

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
        getData(params, token, res, callback);
    }
}
module.exports = chart_area;
