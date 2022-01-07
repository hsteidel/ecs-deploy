package com.hxs.ecsdeploy.commands;

import com.hxs.ecsdeploy.ApplicationException;
import com.hxs.ecsdeploy.aws.EcsClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ecs.model.*;
import software.amazon.awssdk.services.ecs.waiters.EcsWaiter;


@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateServiceCommand {

    private final EcsClientFactory ecsClientFactory;

    public void run(UpdateServiceOptions updateOptions) {
        log.debug("Running update command...{}", updateOptions);

        var ecs = ecsClientFactory.ecsClient(updateOptions.getRegion());
        DescribeServicesResponse describeServicesResponse = ecs.describeServices(DescribeServicesRequest.builder()
                .cluster(updateOptions.getCluster())
                .services(updateOptions.getService())
                .build());

        var service = describeServicesResponse.services().stream()
                .findFirst()
                .orElseThrow(() -> new ApplicationException("service not found!"));

        log.debug("Service {} found", service.serviceArn());

        var currentTaskDefinitionArn = service.taskDefinition();
        log.debug("Current task def: {}", currentTaskDefinitionArn);

        TaskDefinition taskDefinition = ecs.describeTaskDefinition(
                DescribeTaskDefinitionRequest.builder()
                        .taskDefinition(currentTaskDefinitionArn)
                        .build()).taskDefinition();

        ContainerDefinition existingContainerDefinition = taskDefinition.containerDefinitions().stream().findAny().orElseThrow();
        var existingImage = existingContainerDefinition.image();
        var newImage = existingImage.substring(0, existingImage.indexOf(":")) + ":" + updateOptions.getTag();
        log.debug("New image definition: {}", newImage);

        ContainerDefinition newContainerDefinition = existingContainerDefinition.toBuilder()
                .copy()
                .image(newImage)
                .build();

        TaskDefinition newTaskDefinition = ecs.registerTaskDefinition(
                RegisterTaskDefinitionRequest.builder()
                        .cpu(taskDefinition.cpu())
                        .memory(taskDefinition.memory())
                        .family(taskDefinition.family())
                        .ipcMode(taskDefinition.ipcMode())
                        .pidMode(taskDefinition.pidMode())
                        .volumes(taskDefinition.volumes())
                        .networkMode(taskDefinition.networkMode())
                        .taskRoleArn(taskDefinition.taskRoleArn())
                        .containerDefinitions(newContainerDefinition)
                        .ephemeralStorage(taskDefinition.ephemeralStorage())
                        .executionRoleArn(taskDefinition.executionRoleArn())
                        .proxyConfiguration(taskDefinition.proxyConfiguration())
                        .placementConstraints(taskDefinition.placementConstraints())
                        .inferenceAccelerators(taskDefinition.inferenceAccelerators())
                        .requiresCompatibilities(taskDefinition.requiresCompatibilities())
                        .build()).taskDefinition();

        log.debug("Registered new task def: {}", newTaskDefinition.taskDefinitionArn());

        var updatedService = ecs.updateService(
                UpdateServiceRequest.builder()
                        .cluster(updateOptions.getCluster())
                        .service(updateOptions.getService())
                        .taskDefinition(newTaskDefinition.taskDefinitionArn())
                        .build()
        ).service();
        log.info("Updated service with new image tag!");

        log.info("Waiting for service to update successfully....");
        var waiter = EcsWaiter.builder().client(ecs).build();
        var waiterResponse = waiter.waitUntilServicesStable(DescribeServicesRequest.builder()
                        .services(updatedService.serviceArn())
                        .cluster(updateOptions.getCluster())
                .build());
        waiterResponse.matched().response().ifPresent(r -> {
            log.info("Service updated successfully!");
        });

        waiterResponse.matched().exception().ifPresent(t -> {
            log.error("Failed to update service, rolling back: {}", t.getMessage());
            var rolledBackService = ecs.updateService(
                    UpdateServiceRequest.builder()
                            .cluster(updateOptions.getCluster())
                            .service(updateOptions.getService())
                            .taskDefinition(currentTaskDefinitionArn)
                            .build()
            ).service();
            log.info("Rolled back to task def: {}", rolledBackService.taskDefinition());
        });
    }
}
