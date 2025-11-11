package com.viktor.it;

import com.viktor.avro.Device;
import com.viktor.dao.DeviceDao;
import com.viktor.service.device.DeviceProcessor;
import com.viktor.utils.DeviceUtilData;
import com.viktor.utils.ShardingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DeviceProcessorBatchIT extends AbstractIntegrationTest {

    @Autowired
    private DeviceProcessor deviceProcessor;

    @Autowired
    private ShardingUtil shardingUtil;

    @Autowired
    private DeviceDao deviceDao;

    @Test
    @DisplayName("Given multiple devices When batch processed Then each saved to correct shard")
    void shouldProcessBatchAndRouteByShard() {

        Device dev1 = DeviceUtilData.getDevice1();
        Device dev2 = DeviceUtilData.getDevice2();

        List<Device> batch = List.of(dev1, dev2);

        deviceProcessor.processDevices(batch).blockLast(Duration.ofSeconds(10));

        assertThat(deviceDao.findById(dev1.getDeviceId())).isPresent();
        assertThat(deviceDao.findById(dev2.getDeviceId())).isPresent();

        assertThat(shardingUtil.determineShard(dev1.getDeviceId()))
                .isNotEqualTo(shardingUtil.determineShard(dev2.getDeviceId()));

        assertThat(shardingUtil.determineShard(dev2.getDeviceId()))
                .isNotNull();
    }
}
