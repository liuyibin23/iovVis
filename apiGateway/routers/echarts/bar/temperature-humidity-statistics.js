const axios = require('axios');
const util = require('../../../util/utils');

let option = {
    legend: {
        data: ['蓝色预警', '红色预警', '预警百分比'],
        //orient: 'vertical',
        x: 'center',
        y: '2%',
        backgroundColor: '#eee',
        borderColor: 'rgba(178,34,34,0.8)',
        borderWidth: 2
    },
    xAxis: [
        {
            type: 'category',
            data: ['201901', '201902', '201903', '201904', '201905', '201906', '201907', '201908', '201909', '201910'],
        }
    ],
    yAxis: [
        {
            type: 'value',
            min: 0,
            max: 200
        },
        {
            type: 'value',
            min: 0,
            max: 100,
            axisLabel: {
                formatter: '{value} %'
            }
        }
    ],
    series: [
        {
            name: '蓝色预警',
            type: 'bar',
            //barWidth:20 ,
            itemStyle: {        // 系列级个性化样式，纵向渐变填充
                normal: {
                    color: 'blue',
                    label: {
                        show: true,
                        textStyle: {
                            fontFamily: '微软雅黑',
                            fontWeight: 'bold'
                        }
                    }
                }
            },
            data: [30, 20, 70, 60, 23, 38, 93, 36, 35, 37]
        },
        {
            name: '红色预警',
            type: 'bar',
            //barWidth:20,
            barCateGoryGap: 50,
            //barGap: '2',
            itemStyle: {                // 系列级个性化
                normal: {
                    color: 'red',
                    label: {
                        show: true,
                        textStyle: {
                            fontFamily: '微软雅黑',
                            fontWeight: 'bold'
                        }
                    }
                },
            },
            data: [10, 20, 70, 69, 23, 88, 90, 56, 35, 87]
        },
        {
            name: '预警百分比',
            type: 'line',
            yAxisIndex: 1,
            data: [40, 20, 40, 80, 88, 35, 22, 55, 36, 60],
            itemStyle: {        // 系列级个性化样式，纵向渐变填充
                normal: {
                    color: 'green',
                    label: {
                        show: true,
                        textStyle: {
                            fontFamily: '微软雅黑',
                            //fontWeight : 'bold',
                        }
                    }
                }
            }
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
