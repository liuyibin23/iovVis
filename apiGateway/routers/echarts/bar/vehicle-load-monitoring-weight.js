const axios = require('axios');
const util = require('../../../util/utils');

let option = {
    title : {
         text: '车辆荷载自动监测数据_柱状_车重',
         x: 'center',
         y:'bottom'
     },
     legend: {
         data:['车重 > 50T', '20T < 车重 < 50T', '车重 < 20T'],
             orient: 'vertical',
        x:'85%',
         y:'10%',
         backgroundColor: '#eee',
         borderColor: 'rgba(178,34,34,0.8)',
         borderWidth: 2
     },
     toolbox: {
         show : false
     },
     xAxis : [
         {
             type : 'category',
             data : ['1','2','3','4','5','6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18',]
         }
     ],
     yAxis : [
         {
                type : 'value',
           axisLabel : {
                 formatter: '{value} 辆'
             }
         }
     ],
     series : [
         {
             name:'20T < 车重 < 50T',
             type:'bar',
             stack: '总量',
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
             data:[20000, 10000, 10000, 20000, 20000, 20000, 10000, 10000, 20000, 20000, 20000,10000, 10000, 20000, 20000, 20000]
         },
         {
             name:'车重 < 20T',
             type:'bar',
             stack: '总量',
             data:[5000, 5000, 5000, 8000, 8000, 8000, 5000, 5000, 5000, 8000, 8000, 8000,5000, 5000, 5000, 8000, 8000, 8000]
         },
         {
             name:'车重 > 50T',
             type:'bar',
               barWidth:25,
             stack: '总量',
          
             //itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
             data:[3000,5000, 5000, 3000,3000,3000, 8000, 8000, 5000, 5000, 5000, 8000, 8000, 8000, 8000, 5000, 5000, 5000, 8000, 8000, ]
         }
     ]
 };
                     
                    
var chart_area = {
    name: 'chart_data',
    version: '1.0.0',
    fillData: async function (params, token, res, callback) {
        
        let apiUrl = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?limit=100&agg=NONE&keys=crackWidth&startTs=${params.startTime}&endTs=${params.endTime}`;
        axios.get(apiUrl, { headers: { "X-Authorization": token } })
            .then((resp) => {
                let data = resp.data.crackWidth;
                if (data) {
                    
                }
                callback(option, params, res);
            })
            .catch((err) => {
                callback(option, params, res);
            });
    }
}
module.exports = chart_area;
