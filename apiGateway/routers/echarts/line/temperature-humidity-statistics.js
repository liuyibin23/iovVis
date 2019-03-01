const axios = require('axios');
const util  = require('../../../util/utils');
/*
温度/湿度统计
单个传感器
样式：双轴图，湿度+温度
X轴刻度：天
左Y轴单位：摄氏度
右Y轴刻度：百分比数值
聚合方式：最高/最低/平均
多个传感器多张
*/
let option = {
    title : {
        text: '温度/湿度统计',
        x: 'center',
        y:'bottom'
    },
    tooltip : {
        trigger: 'axis'
    },
    legend: {
        data:['最高温度', '平均温度', '最低温度', '最高湿度', '平均湿度', '最低湿度'],
        //orient: 'vertical',
        x:'center',
        y:'top',
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
                formatter: '{value} °C'
            }
        },
      {
         type : 'value',
            // min:0,
            // max:100,
            axisLabel : {
                formatter: '{value} %'
            }
      }
    ],
    series : [
        {
            name:'最高温度',
            type:'line',
            data:[
                //80,20,40,10,30,40,70,80,3, 4, 50,60,80,90,20,10, 10,60,80,90,
            ]
        },
      	{
            name:'平均温度',
            type:'line',
            data:[
                //10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40
            ]
        },
      	{
            name:'最低温度',
            type:'line',
            data:[
                //1,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8
            ]
        },
       {
            name:'最高湿度',
            type:'line',
         	yAxisIndex: 1,
            data:[
                //20,50,20,40,60,80,90,20,10, 10, 20, 5, 30, 1,22,12,25,4,13,23
            ]
        },
       {
            name:'平均湿度',
            type:'line',
         	yAxisIndex: 1,
            data:[
                //10,12,11,6,18,19,10,12,11,6,18,19,10,12,11,6,18,19,10,12,11
            ]
        },
       {
            name:'最低湿度',
            type:'line',
         	yAxisIndex: 1,
            data:[
                //5,6,8,9,1,2,3,6,7,5,6,8,9,1,2,3,6,7,5,6,8,9,1,2,3,6,7,5,6,8,9,1,2,3
            ]
        }
    ]
};

const DataTypeTemperature = 0;   // 温度
const DataTypeHumidity    = 1;   // 湿度

var allData = [];
var MAX_DATA = 6;
var aggList = ['MAX', 'AVG', 'MIN', 'MAX', 'AVG', 'MIN'];
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

    //console.log('end getData');
}

async function getData(idx, dataType, params, token, res, callback){
    //console.log('start getData:' + idx);
    let interval = 10 * 1000;
    let limit    = 1000;
    let keyValue = (dataType == DataTypeTemperature) ? '温度' : '湿度';
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${keyValue}`
     + `&startTs=${params.startTime}&endTs=${params.endTime}&interval=${interval}&limit=${limit}&agg=${aggList[idx]}`;
    api = encodeURI(api);
    //console.log(api);

    await axios.get(api, {
        headers: { "X-Authorization": token }
      }).then(response => {
        //console.log('idx:' + idx + ' return:' + retCnt);
        retCnt++;
        allData[idx] = (idx < 3) ? response.data.温度 :response.data.湿度;

        if (retCnt == MAX_DATA){
            console.log('all data receive');
            processData(res, params, callback);
        }
      }).catch(err => {
        if (!respHasSend) {
            respHasSend = true;
            console.log("err" + err);
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
            
            let dataType = (i < 3) ? DataTypeTemperature : DataTypeHumidity;
            getData(i, dataType, params, token, res, callback);
        }
    }
}
module.exports = chart_area;
