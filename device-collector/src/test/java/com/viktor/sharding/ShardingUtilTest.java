package com.viktor.sharding;

import com.viktor.configs.model.ShardingProperties;
import com.viktor.utils.ShardingUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ShardingUtil.class)
@EnableConfigurationProperties(ShardingProperties.class)
@TestPropertySource("classpath:application-test.yaml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ShardingUtilTest {

    @Autowired
    private ShardingUtil shardingUtil;

    @Test
    void whenDetermineShard_thenResultIsShard0OrShard1() {
        String shard = shardingUtil.determineShard("key123");

        assertThat(shard)
                .isIn("shard0", "shard1");
    }

    @Test
    void givenSameKey_whenDetermineShard_thenReturnsSameShardEachTime() {
        String key = "device-42";

        String s1 = shardingUtil.determineShard(key);
        String s2 = shardingUtil.determineShard(key);
        String s3 = shardingUtil.determineShard(key);

        assertThat(s1).isEqualTo(s2).isEqualTo(s3);
    }

    @Test
    void givenDifferentKeys_whenDetermineShard_thenBothShardsPresent() {
        String shardA = shardingUtil.determineShard("A");
        String shardB = shardingUtil.determineShard("B");

        assertThat(shardA).isIn("shard0", "shard1");
        assertThat(shardB).isIn("shard0", "shard1");

        assertThat(shardA).isNotEqualTo(shardB);
    }
}
