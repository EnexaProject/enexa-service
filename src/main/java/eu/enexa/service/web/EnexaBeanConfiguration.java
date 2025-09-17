package eu.enexa.service.web;

import org.apache.jena.riot.Lang;
import org.dice_research.rdf.spring_jena.JenaModelHttpMessageConverter;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;

@Configuration(proxyBeanMethods = false)
public class EnexaBeanConfiguration {

    @Bean
    public HttpMessageConverters customConverters() {
        HttpMessageConverter<?> textConverter = JenaModelHttpMessageConverter.createForTextMediaTypes(Lang.TURTLE);
        HttpMessageConverter<?> appConverter = JenaModelHttpMessageConverter
                .createForApplicationMediaTypes(Lang.JSONLD);
        return new HttpMessageConverters(textConverter, appConverter);
    }

}
