package kr.co.awesomelead.groupware_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info().title("어썸그룹웨어 API").version("v1.0.0").description("어썸그룹웨어 API 입니다");

        return new OpenAPI()
            .components(new Components())
            .addServersItem(new Server().url("/"))
            .info(info);
    }
}
