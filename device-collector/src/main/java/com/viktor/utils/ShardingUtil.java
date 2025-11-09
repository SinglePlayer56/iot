package com.viktor.utils;

import com.viktor.configs.model.ShardingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ShardingUtil {

    private final ShardingProperties props;

    private static final Pattern MOD_PATTERN =
            Pattern.compile("%\\s*(\\d+)\\s*}");

    public String determineShard(String deviceId) {
        String algorithm = props.shardingAlgorithm();

        // 1. Достаем число из выражения
        int shardCount = extractShardCount(algorithm);

        // 2. Считаем индекс шарда
        int idx = Math.abs(deviceId.hashCode()) % shardCount;

        // 3. Подставляем значение в строку
        return algorithm.replaceAll("\\$\\{.*?}", String.valueOf(idx));
    }

    private int extractShardCount(String expr) {
        var matcher = MOD_PATTERN.matcher(expr);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid sharding expression: " + expr);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
