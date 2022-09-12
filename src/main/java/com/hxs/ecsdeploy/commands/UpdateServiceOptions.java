package com.hxs.ecsdeploy.commands;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateServiceOptions {

    private String tag;

    private String region;

    private String cluster;

    private String service;

    public UpdateServiceOptions cloneWith(String cluster, String service) {
        return UpdateServiceOptions.builder()
                .tag(this.tag)
                .region(this.region)
                .cluster(cluster)
                .service(service)
                .build();
    }
}
