package com.ssarvis.backend.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultAccountBootstrapRunner implements ApplicationRunner {

    private final DefaultAccountBootstrapService defaultAccountBootstrapService;

    public DefaultAccountBootstrapRunner(DefaultAccountBootstrapService defaultAccountBootstrapService) {
        this.defaultAccountBootstrapService = defaultAccountBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        defaultAccountBootstrapService.ensureDefaultAccountResources();
    }
}
