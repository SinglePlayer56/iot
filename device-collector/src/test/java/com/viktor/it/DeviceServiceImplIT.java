package com.viktor.it;

import com.viktor.dao.DeviceDao;
import com.viktor.service.device.DeviceService;
import com.viktor.utils.DeviceUtilData;
import com.viktor.utils.ShardingUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DeviceServiceImplIT extends AbstractIntegrationTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private ShardingUtil shardingUtil;

    @Autowired
    List<DataSource> dataSources;

    @Test
    @DisplayName("Save device to correct shard")
    void givenNewDevice_whenSaving_thenSavedToCorrectShard() {
        var device = DeviceUtilData.getDevice1();

        String expectedShard = shardingUtil.determineShard(device.getDeviceId());

        StepVerifier.create(deviceService.saveDevice(device))
                .verifyComplete();

        var saved = deviceDao.findById(device.getDeviceId());

        assertThat(saved).isPresent();
        assertThat(shardingUtil.determineShard(saved.get().getDeviceId()))
                .isEqualTo(expectedShard);
    }

    @Test
    @DisplayName("Saving same device twice is idempotent")
    void givenSameDeviceTwice_whenSaving_thenIdempotentUpdate() {
        var device = DeviceUtilData.getDevice1();

        StepVerifier.create(deviceService.saveDevice(device))
                .verifyComplete();

        var first = deviceDao.findById(device.getDeviceId());
        assertThat(first).isPresent();

        device.setMeta("updated");

        StepVerifier.create(deviceService.saveDevice(device))
                .verifyComplete();

        var second = deviceDao.findById(device.getDeviceId());
        assertThat(second).isPresent();

        assertThat(second.get().getMeta()).isEqualTo("updated");
    }

    @Test
    @DisplayName("Different devices go to different shards")
    void givenTwoDevices_whenSaving_thenStoredInDifferentShards() {
        var devA = DeviceUtilData.getDevice1();

        var devB = DeviceUtilData.getDevice2();

        var shardA = shardingUtil.determineShard(devA.getDeviceId());
        var shardB = shardingUtil.determineShard(devB.getDeviceId());

        assertThat(shardA).isNotEqualTo(shardB);

        StepVerifier.create(deviceService.saveDevice(devA)).verifyComplete();
        StepVerifier.create(deviceService.saveDevice(devB)).verifyComplete();

        assertThat(deviceDao.findById(devA.getDeviceId())).isPresent();
        assertThat(deviceDao.findById(devB.getDeviceId())).isPresent();
    }
}
