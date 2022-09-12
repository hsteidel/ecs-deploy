package com.hxs.ecsdeploy.aws;

public record EcsServiceDescriptor(
        String clusterName,
        String serviceName
) {

}
