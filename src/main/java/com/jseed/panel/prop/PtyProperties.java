package com.jseed.panel.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "pty")
@Component
@Data
public class PtyProperties {

    private boolean enable;

}
