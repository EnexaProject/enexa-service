package eu.enexa.service.web;

import org.apache.jena.riot.Lang;
import org.dice_research.rdf.spring_jena.JenaModelHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;

@Configuration
public class EnexaBeanConfiguration {

    @Bean
    public HttpMessageConverter<?> getTextConverters() {
        return JenaModelHttpMessageConverter.createForTextMediaTypes(Lang.TURTLE);
    }

    @Bean
    public HttpMessageConverter<?> getApplicationConverters() {
        return JenaModelHttpMessageConverter.createForApplicationMediaTypes(Lang.JSONLD);
    }
}
