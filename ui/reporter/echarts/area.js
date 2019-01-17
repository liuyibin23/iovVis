const axios = require('axios');

let  option = {
    title: {
        text: '堆叠区域图'
    },
    tooltip : {
        trigger: 'axis'
    },
    legend: {
        data:['搜索引擎']
    },
    toolbox: {
        feature: {
            saveAsImage: {}
        }
    },
    grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
    },
    xAxis : [
        {
            type : 'category',
            boundaryGap : false,
            data : []
        }
    ],
    yAxis : [
        {
            type : 'value'
        }
    ],
    series : [
        {
            name:'搜索引擎',
            type:'line',
            stack: '总量',
            label: {
                normal: {
                    show: true,
                    position: 'top'
                }
            },
            areaStyle: {normal: {}},
            data:[]
        }
    ]
};

var chart_data = {
	name: 'chart_data',
	version: '1.0.0',

    fillData: async function(devid, tsw, token) {
        console.log(token);
        let apiUrl = `http://cf.beidouapp.com:8080/api/plugins/telemetry/DEVICE/${devid}/values/timeseries?limit=100&agg=NONE&keys=crackWidth&startTs=1547642072602&endTs=1547643572602`;
        let res = await axios.get(apiUrl, { headers: { "X-Authorization": token } });
        let data = res.data.crackWidth;
        data.forEach((element, index, array) => {
            var val = Math.round(Number.parseFloat(element.value) * 100) / 100;
            option.series[0].data.push(val);
            option.xAxis[0].data.push(index);
        });
        return option;
    }
}
module.exports = chart_data;
