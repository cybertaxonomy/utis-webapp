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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import com.wordnik.swagger.model.ApiInfo;

/**
 * @author a.kohlbecker
 * @date Jul 1, 2014
 *
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"com.mangofactory.swagger.configuration.SpringSwaggerConfig",
        "com.mangofactory.swagger.controllers", "org.bgbm.utis.controller"})
@EnableSwagger
public class SpringMVCConfig extends WebMvcConfigurerAdapter {

    public static final String[] WEB_JAR_RESOURCE_PATTERNS = {"css/", "images/", "lib/", "swagger-ui.js"};
    public static final String WEB_JAR_RESOURCE_LOCATION = "classpath:META-INF/resources/";
    public static final String WEB_JAR_VIEW_RESOLVER_PREFIX = "classpath:/resources/";
    public static final String WEB_JAR_VIEW_RESOLVER_SUFFIX = ".jsp";

    private SpringSwaggerConfig springSwaggerConfig;

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
        configurer.favorPathExtension(true).favorParameter(false).defaultContentType(MediaType.APPLICATION_JSON);
    }


    // -------- Swagger stuff ------------

   /**
    * Required to autowire SpringSwaggerConfig
    */
   @Autowired
   public void setSpringSwaggerConfig(SpringSwaggerConfig springSwaggerConfig) {
      this.springSwaggerConfig = springSwaggerConfig;
   }

   /**
    * Every SwaggerSpringMvcPlugin bean is picked up by the swagger-mvc framework - allowing for multiple
    * swagger groups i.e. same code base multiple swagger resource listings.
    */
   @Bean
   public SwaggerSpringMvcPlugin customImplementation(){
       // includePatterns: If not supplied a single pattern ".*?" is used by SwaggerSpringMvcPlugin
       // which matches anything and hence all RequestMappings. Here we define it explicitely
      return new SwaggerSpringMvcPlugin(this.springSwaggerConfig)
              .apiInfo(apiInfo()).
              includePatterns(".*?");
   }

   private ApiInfo apiInfo() {
       ApiInfo apiInfo = new ApiInfo(
               "EU-BON Utis",
               "The Unified Taxonomic Information Service (UTIS) is the taxonomic backbone for the EU-BON project",
               "UTIS API terms of service",
               "EditSupport@bgbm.org",
               "Mozilla Public License 2.0",
               "http://www.mozilla.org/MPL/2.0/"
         );
       return apiInfo;
     }


}
