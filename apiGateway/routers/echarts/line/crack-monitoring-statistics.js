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

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        var receiveMessage = false;
        tk = token.substr(7);
        const webSocket = new WebSocket('ws://cf.beidouapp.com:8080/api/ws/plugins/telemetry?token=' + tk);
        webSocket.onopen = function open() {
            console.log('webSocket connected!');

            var cmd_sub = {
                tsSubCmds: [],
                historyCmds: [
                    {
                    "entityType": "DEVICE",
                    "entityId": params.devid, //A监测点
                    "keys": 'crackWidth',
                    "startTs": params.startTime,  //距离1970年1月1日零点的毫秒数
                    "endTs": params.endTime,
                    "interval": 5000,			//分组间隔1000毫秒
                    //"limit": 500,
                    //"cmdId": 13,
                    "agg": "AVG"
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
            if (obj.data.crackWidth){
                let data = obj.data.crackWidth;
                data.forEach((element, index, data) => {
                    var val = Math.round(Number.parseFloat(element[1]) * 100) / 100;

                    // 最大裂缝
                    option.series[0].data.push(val);
                    // 平均裂缝
                    option.series[1].data.push(val - 20);
                    // 最小裂缝
                    option.series[2].data.push(val - 40);

                    option.xAxis[0].data.push(index);
                }); 
            }
            
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
