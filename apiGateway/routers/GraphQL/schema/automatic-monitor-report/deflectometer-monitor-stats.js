// 主拱圈挠度自动监测
const axios = require('axios');
const util = require('../../../../util/utils');
var path = require('path'); //系统路径模块
var fs = require('fs'); //文件模块

const {
  GraphQLObjectType,
  GraphQLSchema,
  GraphQLInt,
  GraphQLID,
  GraphQLString,
  GraphQLList,
  GraphQLNonNull,
} = require('graphql');

// 获取数据
async function getDeflectometerMonitorInfoStatsData(req, assetId){
  let data = [
    {no:1, monitorName:'主拱圈挠度自动监测统计表测试数据 底层数据API还未实现', initialValue:'10', preMonitorValue:'10', NowMonitorValue:'15', changeValue:'1', rateOfChange:'10%', warningLevel:'蓝色'},
    {no:2, monitorName:'主拱圈挠度自动监测统计表测试数据 底层数据API还未实现', initialValue:'20', preMonitorValue:'12', NowMonitorValue:'15', changeValue:'1', warningLevel:'12%', warningLevel:'黄色'},
    {no:3, monitorName:'主拱圈挠度自动监测统计表测试数据 底层数据API还未实现', initialValue:'20', preMonitorValue:'25', NowMonitorValue:'20', changeValue:'1', warningLevel:'20%', warningLevel:'红色'}
  ];

  return data;
}

 // 
 const deflectometerMonitorInfo = new GraphQLObjectType({
  name: 'crackMonitoInfoStats',
  fields: {
    // 序号
    no: {
      type: GraphQLString
    },
    // 监测名称
    monitorName: {
      type: GraphQLString
    },
    // 初始值
    initialValue: {
      type: GraphQLString
    },
    // 上次监测值
    preMonitorValue:{
      type: GraphQLString
    },
    // 本次监测值
    NowMonitorValue:{
      type: GraphQLString
    },
     // 变化值
    changeValue:{
      type: GraphQLString
    },
     // 变化率
     rateOfChange:{
      type: GraphQLString
    },
    // 预警级别
    warningLevel:{
      type: GraphQLString
    },
  }
});

const queryObj = new GraphQLObjectType({
name: 'deflectometerMonitorInfoStatsDataQuery',
description: '主拱圈挠度自动监测统计',
fields: {
  deflectometerMonitorInfoStatsData: {
      type: new GraphQLList(deflectometerMonitorInfo),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getDeflectometerMonitorInfoStatsData(req, assetId);
      }
    }
  }
});

module.exports = new GraphQLSchema({query: queryObj});
