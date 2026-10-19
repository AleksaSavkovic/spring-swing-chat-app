package org.chatapp.server.integration.service;

import org.chatapp.server.factory.TestDataHelper;
import org.chatapp.server.integration.config.MyTestConfiguration;
import org.chatapp.server.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@ActiveProfiles("test")
@Import({MyTestConfiguration.class, TestDataHelper.class})
public class SyncServiceIT {
    @Autowired
    SyncService syncService;
    @Autowired
    TestDataHelper testDataHelper;


}
