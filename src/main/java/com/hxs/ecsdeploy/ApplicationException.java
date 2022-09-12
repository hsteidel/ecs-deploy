package com.hxs.ecsdeploy;

import org.springframework.boot.ExitCodeGenerator;

public class ApplicationException extends RuntimeException implements ExitCodeGenerator {

    public ApplicationException(String message) {
        super(message);
    }

    @Override
    public int getExitCode() {
        return 1;
    }

}
