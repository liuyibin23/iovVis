const axios = require('axios');
const util = require('../../../util/utils');
const WebSocket = require('ws');
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
                formatter: '{value} °C'
            }
        },
      {
         type : 'value',
             min:0,
             max:100,
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

function random(lower, upper) {
	return Math.floor(Math.random() * (upper - lower+1)) + lower;
}

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        tk = token.substr(7);
        const webSocket = new WebSocket('ws://cf.beidouapp.com:8080/api/ws/plugins/telemetry?token=' + tk);
        
        var receiveMessage = false;
        webSocket.onopen = function open() {
            console.log('webSocket connected!');

            var cmd_sub = {
                tsSubCmds: [],
                historyCmds: [
                    {
                    "entityType": "DEVICE",
                    "entityId": params.devid, //A监测点
                    "keys": '温度',
                    "startTs": params.startTime,  //距离1970年1月1日零点的毫秒数
                    "endTs": params.endTime,
                    "interval": 1000,			//分组间隔1000毫秒
                    //"limit": 500,
                    //"cmdId": 13,
                    "agg": "MAX"
                    }
                ],
                attrSubCmds: []
            };
            var data = JSON.stringify(cmd_sub);
            webSocket.send(data);
        };
        webSocket.onmessage = function incoming(event) {
            receiveMessage = true;
            var obj = JSON.parse(event.data);
            console.log(event.data);
            // console.log('cmdChannel: %d',obj.subscriptionId, obj.data);
            callback(option, params, res);
        };
        webSocket.onclose = function close(){
            console.log('webSocket closed!');
        };

        webSocket.onerror = function error(msg){
            console.log('webSocket errr!');
        }

        setTimeout(timerfun, util.getWsTimeout());
        function timerfun() {
            //console.log('>>>>>Message is sent: ' + receiveMessage);
            if (!receiveMessage) {
                callback(option, params, res);
            }            
        }
    }
}
module.exports = chart_area;
