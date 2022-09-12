package com.hxs.ecsdeploy.commands;

import com.hxs.ecsdeploy.aws.EcsClientFactory;
import com.hxs.ecsdeploy.aws.EcsServiceFinder;
import com.hxs.ecsdeploy.aws.SsmParamClientFactory;
import com.hxs.ecsdeploy.options.CommandOptionsFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHandler {

    private final Options options;

    private final EcsClientFactory ecsClientFactory;

    private final EcsServiceFinder ecsServiceFinder;

    private final CommandOptionsFactory commandOptionsFactory;

    private final SsmParamClientFactory ssmParamClientFactory;


    public void handleArgs(String[] args) {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter helper = new HelpFormatter();
        try {
            CommandLine cmd = parser.parse(options, args);
            var updateOptions = commandOptionsFactory.build(cmd);
            var ecs = ecsClientFactory.withRegion(updateOptions.getRegion());
            var ssm = ssmParamClientFactory.withRegion(updateOptions.getRegion());
            new UpdateServiceCommand(ssm, ecs, ecsServiceFinder).run(updateOptions);
        } catch (ParseException e) {
            log.error("\n{}", e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(1);
        }
    }
}
