package com.hxs.ecsdeploy.aws;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecs.EcsClient;

@Component
public class EcsClientFactory {

    public EcsClient ecsClient(String region) {
        return EcsClient.builder()
                .region(Region.of(region))
                .build();
    }

}
