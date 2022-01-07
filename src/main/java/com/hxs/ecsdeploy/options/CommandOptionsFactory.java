package com.hxs.ecsdeploy.options;

import com.hxs.ecsdeploy.commands.UpdateServiceOptions;
import org.apache.commons.cli.CommandLine;
import org.springframework.stereotype.Component;

@Component
public class CommandOptionsFactory {

    public UpdateServiceOptions build(CommandLine cmd) {
        return UpdateServiceOptions.builder()
                .cluster(cmd.getOptionValue("cluster"))
                .service(cmd.getOptionValue("service"))
                .tag(cmd.getOptionValue("tag"))
                .region(cmd.getOptionValue("region") != null
                        ? cmd.getOptionValue("region")
                        : "us-east-1")
                .build();
    }

}
