/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.cache;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashSet;
import java.util.Set;

@Configuration
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis", matchIfMissing = false)
@EnableCaching
@Data
public class TBRedisCacheConfiguration {

    @Value("${redis.connection.host}")
    private String host;

    @Value("${redis.connection.port}")
    private Integer port;

    @Value("${redis.connection.db}")
    private Integer db;

    @Value("${redis.connection.password}")
    private String password;

    @Value("${redis.connection.clusterNode}")
    private String clusterNode;

    @Bean
    @ConditionalOnProperty(prefix = "redis.connection", value = "type", havingValue = "standalone", matchIfMissing = false)
    public RedisConnectionFactory redisConnectionFactory() {
        JedisConnectionFactory factory = new JedisConnectionFactory();
        factory.setHostName(host);
        factory.setPort(port);
        factory.setDatabase(db);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    @ConditionalOnProperty(prefix = "redis.connection", value = "type", havingValue = "cluster", matchIfMissing = false)
    public RedisClusterConfiguration getRedisCluster() {
        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
        String [] serverArray=clusterNode.split(",");
        Set<RedisNode> jedisClusterNodes = new HashSet<RedisNode>();
        for (String ipPort:serverArray){
            String [] ipPortPair=ipPort.split(":");
            jedisClusterNodes.add(new RedisNode(ipPortPair[0],Integer.valueOf(ipPortPair[1].trim())));
        }
//        jedisClusterNodes.add(new RedisNode("192.168.1.142", 7000));
//        jedisClusterNodes.add(new RedisNode("192.168.1.142", 7001));
//        jedisClusterNodes.add(new RedisNode("192.168.1.142", 7002));
//        jedisClusterNodes.add(new RedisNode("192.168.1.142", 7003));
//        jedisClusterNodes.add(new RedisNode("192.168.1.142", 7004));
//        jedisClusterNodes.add(new RedisNode("192.168.1.142", 7005));
        redisClusterConfiguration.setClusterNodes(jedisClusterNodes);
        return redisClusterConfiguration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "redis.connection", value = "type", havingValue = "cluster", matchIfMissing = false)
    public RedisConnectionFactory redisClusterConnectionFactory(RedisClusterConfiguration redisClusterConfiguration){
        JedisConnectionFactory factory = new JedisConnectionFactory(redisClusterConfiguration);
//        redisConnectionFactory.setPoolConfig(jedisPoolConfig);
        factory.setDatabase(db);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(cf);
        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager(RedisTemplate redisTemplate) {
        return new RedisCacheManager(redisTemplate);
    }

    @Bean
    public KeyGenerator previousDeviceCredentialsId() {
        return new PreviousDeviceCredentialsIdKeyGenerator();
    }


}
