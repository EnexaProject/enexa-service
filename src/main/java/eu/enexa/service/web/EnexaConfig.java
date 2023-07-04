package eu.enexa.service.web;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;

public class EnexaConfig {

    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // TODO Figure out how this method is called
        configurer.favorParameter(false);
        configurer.ignoreAcceptHeader(false);
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
        configurer.mediaType("xml", MediaType.APPLICATION_XML); // --> translate to RDF/XML
        configurer.mediaType("json", MediaType.APPLICATION_JSON); // -- translate to JSON-LD
        // TODO Add RDF media types from the RDF media type utils class.
    }
}
