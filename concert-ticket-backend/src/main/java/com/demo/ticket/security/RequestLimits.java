package com.demo.ticket.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "security.limits")
@Data
public class RequestLimits {
    private int windowSeconds = 60;
    private int apiPerIp = 300;
    private int loginPerIp = 20;
    private int loginPerAccount = 10;
    private int registerPerIp = 5;
    private int refreshPerIp = 60;
    private int refreshPerSession = 20;
    private int bookingPerUser = 20;
    private int wsPerIp = 120;
    private int wsConnectPerUser = 10;
    private int wsMessagesPerUser = 120;
}
