package com.hxs.ecsdeploy.commands;

import com.hxs.ecsdeploy.ApplicationException;
import com.hxs.ecsdeploy.aws.EcsServiceFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;
import software.amazon.awssdk.services.ecs.waiters.EcsWaiter;
import software.amazon.awssdk.services.ssm.SsmClient;


@Slf4j
@RequiredArgsConstructor
public class UpdateServiceCommand {

    private final SsmClient ssm;

    private final EcsClient ecs;

    private final EcsServiceFinder ecsServiceFinder;

    public void run(UpdateServiceOptions updateOptions) {
        log.debug("Running update command...{}", updateOptions);
        var serviceDescriptor = ecsServiceFinder.find(ssm, updateOptions.getCluster(), updateOptions.getService());
        updateService(updateOptions.cloneWith(serviceDescriptor.clusterName(), serviceDescriptor.serviceName()));
    }

    private void updateService(UpdateServiceOptions updateOptions) {
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

        var clonedTaskedDefinition = cloneAndUpdateTaskDefinitionImage(currentTaskDefinitionArn, updateOptions.getTag());
        var updatedServiceInfo = updateService(clonedTaskedDefinition, updateOptions);

        log.info("Waiting for service to update successfully....");
        waitForServiceUpdateOrRollback(updateOptions, updatedServiceInfo, currentTaskDefinitionArn);
    }

    private UpdatedServiceInfo updateService(RegisterTaskDefinitionRequest clonedTaskedDefinition, UpdateServiceOptions updateOptions) {
        TaskDefinition newTaskDefinition = ecs.registerTaskDefinition(clonedTaskedDefinition).taskDefinition();
        log.debug("Registered new task def: {}", newTaskDefinition.taskDefinitionArn());

        var updatedService = ecs.updateService(
                UpdateServiceRequest.builder()
                        .cluster(updateOptions.getCluster())
                        .service(updateOptions.getService())
                        .taskDefinition(newTaskDefinition.taskDefinitionArn())
                        .build()
        ).service();
        log.info("Updated service with new image tag! {}", updateOptions.getTag());
        return new UpdatedServiceInfo(updatedService.serviceArn(), newTaskDefinition.taskDefinitionArn());
    }

    record UpdatedServiceInfo(
            String newServiceArn,
            String newTaskDefinitionArn
    ) {

    }

    private void waitForServiceUpdateOrRollback(UpdateServiceOptions updateOptions, UpdatedServiceInfo updatedServiceInfo, String currentTaskDefinitionArn) {
        var waiter = EcsWaiter.builder().client(ecs).build();
        try {
            var waiterResponse = waiter.waitUntilServicesStable(DescribeServicesRequest.builder()
                    .services(updatedServiceInfo.newServiceArn)
                    .cluster(updateOptions.getCluster())
                    .build());
            waiterResponse.matched().response().ifPresent(r ->
                    log.info("Service updated successfully!")
            );
            waiterResponse.matched().exception().ifPresent(t -> {
                log.error("Failed to update service, rolling back: {}", t.getMessage());
                rollback(ecs, updateOptions, currentTaskDefinitionArn, updatedServiceInfo.newTaskDefinitionArn);
            });
        } catch (Exception e) {
            log.error("Failed to update service, rolling back: {}", e.getMessage());
            rollback(ecs, updateOptions, currentTaskDefinitionArn, updatedServiceInfo.newTaskDefinitionArn);
        }

    }

    private void rollback(EcsClient ecs, UpdateServiceOptions updateOptions, String currentTaskDefinitionArn, String clonedTaskedDefinitionArn) {
        var rolledBackService = ecs.updateService(
                UpdateServiceRequest.builder()
                        .cluster(updateOptions.getCluster())
                        .service(updateOptions.getService())
                        .taskDefinition(currentTaskDefinitionArn)
                        .build()
        ).service();
        ecs.deregisterTaskDefinition(
                DeregisterTaskDefinitionRequest.builder()
                        .taskDefinition(clonedTaskedDefinitionArn)
                        .build()
        );
        log.info("Rolled back to task def: {}", rolledBackService.taskDefinition());
    }

    private RegisterTaskDefinitionRequest cloneAndUpdateTaskDefinitionImage(String currentTaskDefinitionArn, String newImageTag) {
        TaskDefinition taskDefinition = ecs.describeTaskDefinition(
                DescribeTaskDefinitionRequest.builder()
                        .taskDefinition(currentTaskDefinitionArn)
                        .build()).taskDefinition();

        ContainerDefinition existingContainerDefinition = taskDefinition.containerDefinitions().stream().findAny().orElseThrow();
        var existingImage = existingContainerDefinition.image();
        var newImage = existingImage.substring(0, existingImage.indexOf(":")) + ":" + newImageTag;
        log.debug("New image definition: {}", newImage);

        ContainerDefinition newContainerDefinition = existingContainerDefinition.toBuilder()
                .copy()
                .image(newImage)
                .build();

        return RegisterTaskDefinitionRequest.builder()
                .cpu(taskDefinition.cpu())
                .memory(taskDefinition.memory())
                .family(taskDefinition.family())
                .ipcMode(taskDefinition.ipcMode())
                .pidMode(taskDefinition.pidMode())
                .volumes(taskDefinition.volumes())
                .networkMode(taskDefinition.networkMode())
                .taskRoleArn(taskDefinition.taskRoleArn())
                .containerDefinitions(newContainerDefinition)
                .runtimePlatform(taskDefinition.runtimePlatform())
                .ephemeralStorage(taskDefinition.ephemeralStorage())
                .executionRoleArn(taskDefinition.executionRoleArn())
                .proxyConfiguration(taskDefinition.proxyConfiguration())
                .placementConstraints(taskDefinition.placementConstraints())
                .inferenceAccelerators(taskDefinition.inferenceAccelerators())
                .requiresCompatibilities(taskDefinition.requiresCompatibilities())
                .build();
    }

}
