package com.hxs.ecsdeploy.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

import java.util.Optional;


@Slf4j
@Component
public class EcsServiceFinder {

    public EcsServiceDescriptor find(SsmClient ssm, String cluster, String service) {
        String ecsClusterName = discoverClusterName(ssm, cluster);
        String ecsServiceName = discoverServiceName(ssm, service);
        return new EcsServiceDescriptor(ecsClusterName, ecsServiceName);
    }

    private String discoverClusterName(SsmClient ssm, String cluster) {
        return findSsmParamValue(ssm, cluster)
                .orElse(cluster);
    }

    private String discoverServiceName(SsmClient ssm, String service) {
        return findSsmParamValue(ssm, service)
                .orElse(service);
    }

    private Optional<String> findSsmParamValue(SsmClient ssm, String paramName) {
        try {
            var getParamResponse = ssm.getParameter(
                    GetParameterRequest.builder()
                            .name(paramName)
                            .build()
            );
            log.debug("Found param {}", paramName);
            return Optional.ofNullable(getParamResponse.parameter().value())
                    .map(String::trim);
        } catch (ParameterNotFoundException pnfe) {
            log.warn("Could not find SSM param");
            return Optional.empty();
        }
    }
}
