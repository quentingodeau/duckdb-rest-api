package fr.qgo.duckdbrestapi.bean;

import net.bytebuddy.ByteBuddy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppBeans {
    @Bean
    public ByteBuddy byteBuddy() {
        return new ByteBuddy();
    }
}
