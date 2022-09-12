package com.hxs.ecsdeploy.aws;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

@Component
public class SsmParamClientFactory {

    public SsmClient withRegion(String region) {
        return SsmClient.builder()
                .region(Region.of(region))
                .build();
    }

}
