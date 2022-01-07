package com.hxs.ecsdeploy.commands;

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

    private final UpdateServiceCommand updateServiceCommand;

    private final CommandOptionsFactory commandOptionsFactory;

    public void handleArgs(String[] args) {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter helper = new HelpFormatter();
        try {
            CommandLine cmd = parser.parse(options, args);
            var updateOptions = commandOptionsFactory.build(cmd);
            updateServiceCommand.run(updateOptions);
        } catch (ParseException e) {
            log.error("\n{}", e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(1);
        }
    }
}
