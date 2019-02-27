const axios = require('axios');
const util = require('../../../util/utils');

let option = {
    title : {
        text: '应力应变监测数据统计分析',
        x: 'center',
        y:'bottom'
    },
    legend: {
        data:['最大应变', '平均应变', '最小应变'],
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
            data : [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31]
        }
    ],
    yAxis : [
       {
            type : 'value',
         	min:-72,
        	max:60,
            axisLabel : {
                formatter: '{value} Mp'
            }
        }
    ],
    series : [
        {
            name:'最大应变',
            type:'line',
            data:[80,20,40,10,30,40,70,80,3, 4, 50,60,80,90,20,10, 10,60,80,90,40,70,80,3, 4, 50,60,80,90,20,10, ]
        },
      	{
            name:'平均应变',
            type:'line',
            data:[10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40,10,20,30,40,1,20,30,40,10,20,30,40,10,20]
        },
      	{
            name:'最小应变',
            type:'line',
            data:[1,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8,2,3,6,7,8,2,2,5,-1,-3,2,4,5,1,2,3,6,7,8]
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
                    callback(option, params, res);
                }
                else {
                    callback(null, params, res);
                }
            })
            .catch((err) => {
                callback(null, params, res);
            });
    }
}
module.exports = chart_area;
