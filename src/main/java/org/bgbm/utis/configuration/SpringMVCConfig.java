// $Id$
/**
* Copyright (C) 2014 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package org.bgbm.utis.configuration;

import static springfox.documentation.builders.PathSelectors.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cybertaxonomy.utis.tnr.msg.TnrMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.XmlViewResolver;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;
import org.springframework.web.servlet.view.xml.MarshallingView;

import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author a.kohlbecker
 * @date Jul 1, 2014
 *
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "com.mangofactory.swagger.configuration.SpringSwaggerConfig",
        "com.mangofactory.swagger.controllers",
        "org.bgbm.utis.controller"
        })
@EnableSwagger2
public class SpringMVCConfig extends WebMvcConfigurerAdapter {

    public static final String[] WEB_JAR_RESOURCE_PATTERNS = {"css/", "images/", "lib/", "swagger-ui.js"};
    public static final String WEB_JAR_RESOURCE_LOCATION = "classpath:META-INF/resources/";
    public static final String WEB_JAR_VIEW_RESOLVER_PREFIX = "classpath:/resources/";
    public static final String WEB_JAR_VIEW_RESOLVER_SUFFIX = ".jsp";

    private static final Logger log = LogManager.getLogger(SpringMVCConfig.class.getName());

    @Autowired
    protected ServletContext servletContext;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler(WEB_JAR_RESOURCE_PATTERNS)
              .addResourceLocations("/")
              .addResourceLocations(WEB_JAR_RESOURCE_LOCATION).setCachePeriod(0);
    }

    @Bean
    public InternalResourceViewResolver getInternalResourceViewResolver() {
      InternalResourceViewResolver resolver = new InternalResourceViewResolver();
      resolver.setPrefix(WEB_JAR_VIEW_RESOLVER_PREFIX);
      resolver.setSuffix(WEB_JAR_VIEW_RESOLVER_SUFFIX);
      return resolver;
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
      configurer.enable();
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(true).favorParameter(false)
        .defaultContentType(MediaType.APPLICATION_JSON)
        .mediaType("xml", MediaType.APPLICATION_XML)
        .mediaType("json", MediaType.APPLICATION_JSON);
    }

    /**
     * Create the CNVR.  Specify the view resolvers to use explicitly.  Get Spring to inject
     * the ContentNegotiationManager created by the configurer (see previous method).
     */
   @Bean
   public ViewResolver contentNegotiatingViewResolver(ContentNegotiationManager manager) {

       log.info("CNVR");

     // Define the view resolvers
       List<ViewResolver> resolvers = new ArrayList<ViewResolver>();

//       resolvers.add(getXmlViewResolver());
       resolvers.add(getJsonViewResolver());
       resolvers.add(getMarshallingXmlViewResolver());

       ContentNegotiatingViewResolver resolver = new ContentNegotiatingViewResolver();
       resolver.setContentNegotiationManager(manager);
       resolver.setViewResolvers(resolvers);
       return resolver;
   }


    private ViewResolver getXmlViewResolver() {
        XmlViewResolver resolver = new XmlViewResolver();
        resolver.setLocation(new ServletContextResource(servletContext, "/WEB-INF/views.xml"));
        return resolver;
    }

    private ViewResolver getJsonViewResolver() {
        return new ViewResolver() {

            /**
             * Get the view to use.
             *
             * @return Always returns an instance of
             *         {@link MappingJacksonJsonView}.
             */
            @Override
            public View resolveViewName(String viewName, Locale locale) throws Exception {
                MappingJacksonJsonView view = new MappingJacksonJsonView();
                view.setPrettyPrint(true);

                return view;
            }
        };
    }

   /**
    * @return
    */
   private ViewResolver getMarshallingXmlViewResolver() {

       final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();

       marshaller.setPackagesToScan(new String[]{TnrMsg.class.getPackage().getName()});

       return new ViewResolver() {

        @Override
        public View resolveViewName(String viewName, Locale locale) throws Exception {
            MarshallingView view = new MarshallingView();
            view.setMarshaller(marshaller);
            return view;
        }

       };
   }

    // -------- Swagger configuration ------------ //

   @Bean
   public Docket customImplementation(){
       return new Docket(DocumentationType.SWAGGER_2)
               .select()
               .paths(regex("/.*"))
               .build()
               .apiInfo(apiInfo());
   }

   private ApiInfo apiInfo() {

     ApiInfo apiInfo = new ApiInfo(
             "EU BON UTIS",
             "The Unified Taxonomic Information Service (UTIS) is the taxonomic backbone for the EU-BON project.",
             "1.2", //TODO obtain generically?
             "https://www.biodiversitycatalogue.org/services/79",
             new Contact(null, null, "EditSupport@bgbm.org"),
             "Mozilla Public License 2.0",
             "http://www.mozilla.org/MPL/2.0/");
     return apiInfo;
   }


}
