package org.chatapp.server.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties("storage")
@Configuration
public class StorageProperties {

    @Value("${app.upload.dir}")
    private String location;
}
