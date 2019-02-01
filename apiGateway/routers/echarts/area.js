const axios = require('axios');


let option = {
    title: {
        text: ''
    },
    tooltip: {
        trigger: 'axis'
    },
    legend: {
        data: ['']
    },
    toolbox: {
    },
    grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
    },
    xAxis: [
        {
            type: 'category',
            boundaryGap: false,
            data: []
        }
    ],
    yAxis: [
        {
            type: 'value'
        }
    ],
    series: [
        {
            name: '搜索引擎',
            type: 'line',
            stack: '总量',
            label: {
                normal: {
                    show: true,
                    position: 'top'
                }
            },
            areaStyle: { normal: {} },
            data: []
        }
    ]
};

var chart_area = {
    name: 'chart_data',
    version: '1.0.0',

    fillData: async function (params, token, res, callback) {
        let apiUrl = `http://cf.beidouapp.com:8080/api/plugins/telemetry/DEVICE/${params.devid}/values/timeseries?limit=100&agg=NONE&keys=crackWidth&startTs=${params.startTime}&endTs=${params.endTime}`;
        axios.get(apiUrl, { headers: { "X-Authorization": token } })
            .then((resp) => {
                let data = resp.data.crackWidth;
                if (data) {
                    option.title.text = '设备ID: ' + params.devid;
                    data.forEach((element, index, data) => {
                        var val = Math.round(Number.parseFloat(element.value) * 100) / 100;
                        option.series[0].data.push(val);
                        option.xAxis[0].data.push(index);
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
