package com.hxs.ecsdeploy;

import com.hxs.ecsdeploy.commands.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Arrays;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class EcsDeployApplication implements CommandLineRunner {

    private final CommandHandler commandHandler;

    public static void main(String[] args) {
        turnDebugOnIfNedded(args);
        new SpringApplicationBuilder()
                .bannerMode(Banner.Mode.OFF)
                .sources(EcsDeployApplication.class)
                .run(args);
    }

    private static void turnDebugOnIfNedded(String[] args) {
        if (args != null && Arrays.asList(args).contains("debug")) {
            System.setProperty("logging.level.com.hxs", "debug");
        }
    }

    @Override
    public void run(String... args) throws Exception {
        commandHandler.handleArgs(args);
    }
}
