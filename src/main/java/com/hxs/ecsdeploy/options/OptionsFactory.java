package com.hxs.ecsdeploy.options;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OptionsFactory {

    @Bean
    Options buildOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder("c")
                        .longOpt("cluster")
                        .required()
                        .hasArg()
                        .desc("target cluster arn or name")
                        .build()
        );

        options.addOption(
                Option.builder("s")
                        .longOpt("service")
                        .required()
                        .hasArg()
                        .desc("target service arn or name")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt("tag")
                        .required()
                        .hasArg()
                        .desc("docker image tag to deploy")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .longOpt("region")
                        .hasArg()
                        .desc("target aws region")
                        .build()
        );

        return options;
    }

}
