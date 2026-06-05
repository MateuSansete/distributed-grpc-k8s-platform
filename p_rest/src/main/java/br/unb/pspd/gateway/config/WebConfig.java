package br.unb.pspd.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import br.unb.pspd.gateway.controller.PackageSearchController;

/**
 * CORS para o frontend (hclient/) que roda em outra origem (ex.: http://localhost:5500).
 * Expõe o header X-Search-Time-Ms para o JS conseguir lê-lo via fetch.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*") // acadêmico: aceita qualquer origem local
                .allowedMethods("GET")
                .exposedHeaders(PackageSearchController.SEARCH_TIME_HEADER);
    }
}
