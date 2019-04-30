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
            data: [],
        }
    ],
    yAxis: [
        {
            type: 'value'
        },
        {
            type: 'value',
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
                        position: 'top',
                        textStyle: {
                            fontFamily: '微软雅黑',
                            fontWeight: 'bold'
                        }
                    }
                }
            },
            data: []
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
                        position: 'top',
                        textStyle: {
                            fontFamily: '微软雅黑',
                            fontWeight: 'bold'
                        }
                    }
                },
            },
            data: []
        },
        {
            name: '预警百分比',
            type: 'line',
            yAxisIndex: 1,
            data: [],
            itemStyle: {        // 系列级个性化样式，纵向渐变填充
                normal: {
                    color: 'green',
                    label: {
                        show: true,
                        formatter: '{c}' + '%',
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


function processData(ts, option, params, respCnt, idx, maxCnt, data, res, callback) {
    let blueCnt = 0;
    let redCnt = 0;
    for (let i = 0; i < data.length; i++) {
        let _dt = data[i];

        if (_dt.severity === 'INDETERMINATE') {
            blueCnt += _dt.alarmCount;
        } else if (_dt.severity === 'WARNING') {
            redCnt += _dt.alarmCount;
        }
    }
    
    if (data.length >  0) {
        option.xAxis[0].data[idx] = ts;
        option.series[0].data[idx] = blueCnt;
        option.series[1].data[idx] = redCnt;
        option.series[2].data[idx] = (redCnt + blueCnt) > 0 ? ((redCnt / (redCnt + blueCnt)) * 100).toFixed(2) : 0;
    }
    
    if (respCnt == maxCnt) {
        callback(option, params, res);
    }    
}



async function getData(timeSeries, params, token, res, callback){
    var respCnt = 0;
    var validIdx = 0;
    for (let i = 0; i < timeSeries.length; i++) {
        let api = util.getAPI() + `currentUser/alarms/${params.devid}?limit=1000&startTime=${timeSeries[i][0]}&endTime=${timeSeries[i][1]}`;
        await axios.get(api, { headers: { "X-Authorization": token } })
            .then((resp) => {
                respCnt++;
                let data = resp.data.data;
                if (data) {
                    var dat = new Date(timeSeries[i][0]);
                    let ts = util.dateFormat(dat,'yyyyMMdd');
                    processData(ts, option, params, respCnt, validIdx, timeSeries.length, data, res, callback);
                    if (data.length > 0)
                        validIdx++;
                }
            })
            .catch((err) => {
                //callback(option, params, res);
            });
    } 
}

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',
    fillData: async function (params, token, res, callback) {
        option.xAxis[0].data = [];
        option.series[0].data = [];
        option.series[1].data = [];
        option.series[2].data = [];

        // 计算时间边界
        let timediff = params.endTime - params.startTime;
        let interval = Number.parseFloat(params.interval) * 1000;
        let loopCnt =  Math.ceil((params.endTime - params.startTime) / interval);
        
        let timeSeries = [];
        // 按时间段拆分成多个组 多少个柱状
        let startTimeFloat = Number.parseFloat(params.startTime);
        for (let j = 0; j < loopCnt; j++) {        
            let startTime = startTimeFloat + j * interval;
            let endTime   = startTime + interval;
            timeSeries.push([startTime, endTime]);
        }

        getData(timeSeries, params, token, res, callback);
    }
}
module.exports = chart_area;
