// 索力数据量统计分析
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
async function getCableForceAnalysisStatsData(req, assetId){
  let data = [
    {no:1, occurrenceTime:'索力数据量统计分析测试数据 底层数据API还未实现', measureValue:'8',  designValue:'5', threshold:'10', perOverThreshold:'20%'},
    {no:2, occurrenceTime:'索力数据量统计分析测试数据 底层数据API还未实现', measureValue:'20', designValue:'10', threshold:'20', perOverThreshold:'30%'},
    {no:3, occurrenceTime:'索力数据量统计分析测试数据 底层数据API还未实现', measureValue:'30', designValue:'15', threshold:'30', perOverThreshold:'50%'}
  ];

  return data;
}

 // 索力数据量统计分析
 const cableForceAnalysisStats = new GraphQLObjectType({
  name: 'cableForceAnalysisStats',
  fields: {
    // 序号
    no: {
      type: GraphQLString
    },
    // 发生时间
    occurrenceTime: {
      type: GraphQLString
    },
    // 测值
    measureValue: {
      type: GraphQLString
    },
    // 设计值
    designValue:{
      type: GraphQLString
    },
    // 阈值
    threshold:{
      type: GraphQLString
    },
    //超阈值百分比
    perOverThreshold:{
       type: GraphQLString
    }
  }
});

const queryObj = new GraphQLObjectType({
name: 'cableForceAnalysisStatsQuery',
description: '索力数据量统计分析',
fields: {
  cableForceAnalysisStatsData: {
      type: new GraphQLList(cableForceAnalysisStats),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getCableForceAnalysisStatsData(req, assetId);
      }
    }
  }
});

module.exports = new GraphQLSchema({query: queryObj});
