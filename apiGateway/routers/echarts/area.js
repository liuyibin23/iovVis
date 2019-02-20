const axios = require('axios');
const util = require('../../util/utils');

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
        data:['温度', '湿度'],
        orient: 'horizontal',
        x:'80%',
        y:'5%',
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
            data : []
        }
    ],
    yAxis : [
        {
            type : 'value',
            // min:0,
            // max:100,
            axisLabel : {
                formatter: '{value} °C'
            }
        }
    ],
    series : [
        {
            name:'温度',
            type:'line',
            data:[],
            markLine : {
                data : [
                    {type : 'average', name: '平均值'}
                ]
            }
        },
        {
            name:'湿度',
            type:'line',
            data:[],
            markLine : {
                data : [
                    {type : 'average', name: '平均值'}
                ]
            }
        },
    ]
};

function random(lower, upper) {
	return Math.floor(Math.random() * (upper - lower+1)) + lower;
}

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        
        let apiUrl = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?limit=100&agg=NONE&keys=crackWidth&startTs=${params.startTime}&endTs=${params.endTime}`;
        axios.get(apiUrl, { headers: { "X-Authorization": token } })
            .then((resp) => {
                let data = resp.data.crackWidth;
                if (data) {
                    //option.title.text = '设备ID: ' + params.devid;
                    Math.seed
                    data.forEach((element, index, data) => {
                        var val = Math.round(Number.parseFloat(element.value) * 100) / 100 - random(20, 50);
                        option.series[0].data.push(val);
                        option.xAxis[0].data.push(index);

                        var val2 = Math.round(Number.parseFloat(element.value) * 100) / 100 - random(60, 90);
                        option.series[1].data.push(val2);
                    });
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
