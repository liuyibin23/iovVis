package com.beidouapp.server.fileserver.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 自行修改为自己的包路径
                .apis(RequestHandlerSelectors.basePackage("com.beidouapp.server.fileserver.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("api文档")
                .description("restfull 风格接口")
                .version("1.0")
                .build();
    }

//    @Bean
//    @Primary
//    public SwaggerResourcesProvider swaggerResourcesProvider(InMemorySwaggerResourcesProvider defaultResourcesProvider){
//        return () -> {
//            SwaggerResource resource = new SwaggerResource();
//            resource.setName("swagger使用 API");
//            resource.setSwaggerVersion("2.0");
//            resource.setLocation("/swagger.yml");
//            List<SwaggerResource> resourcesList = new ArrayList<>(defaultResourcesProvider.get());
//            resourcesList.clear();
//            resourcesList.add(0, resource);
//            return resourcesList;
//        };
//    }
}
