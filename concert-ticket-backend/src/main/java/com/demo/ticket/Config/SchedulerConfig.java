// 閱讀導引：提供排程執行所需的 Spring 元件，供付款期限等背景任務使用。
package com.demo.ticket.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@org.springframework.scheduling.annotation.EnableScheduling
public class SchedulerConfig {

    /**
     * 建立含 5 個工作執行緒的付款排程器，執行緒名稱以 booking-expire- 開頭，供付款提醒與逾期任務注入使用。
     */
    @Bean("bookingTaskScheduler")
    public TaskScheduler bookingTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("booking-expire-");
        scheduler.initialize();
        return scheduler;
    }
}
