package com.viktor.sharding;

import com.viktor.utils.ShardingUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ShardingUtilTest {

    @Test
    void whenDetermineShard_thenResultIsShard0OrShard1() {
        String shard = ShardingUtil.determineShard("key123");

        assertThat(shard)
                .isIn("shard0", "shard1");
    }

    @Test
    void givenSameKey_whenDetermineShard_thenReturnsSameShardEachTime() {
        String key = "device-42";

        String s1 = ShardingUtil.determineShard(key);
        String s2 = ShardingUtil.determineShard(key);
        String s3 = ShardingUtil.determineShard(key);

        assertThat(s1).isEqualTo(s2).isEqualTo(s3);
    }

    @Test
    void givenDifferentKeys_whenDetermineShard_thenBothShardsPresent() {
        String shardA = ShardingUtil.determineShard("A");
        String shardB = ShardingUtil.determineShard("B");

        assertThat(shardA).isIn("shard0", "shard1");
        assertThat(shardB).isIn("shard0", "shard1");

        assertThat(shardA).isNotEqualTo(shardB);
    }
}
