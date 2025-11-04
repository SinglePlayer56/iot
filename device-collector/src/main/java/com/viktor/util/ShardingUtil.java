package com.viktor.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ShardingUtil {

    public static String determineShard(String shardingKey) {
        return "shard" + Math.abs(shardingKey.hashCode()) % 2;
    }
}
