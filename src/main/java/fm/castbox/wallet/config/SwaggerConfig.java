package fm.castbox.wallet.config;

import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig implements WebMvcConfigurer {

  @Bean
  public Docket api() {
    ApiInfo apiInfo = new ApiInfoBuilder()
        .title("CastBox wallet RESTful API")
        .description("CastBox wallet RESTful API that supports Ethereum and all ERC20 tokens")
        .build();
    return new Docket(DocumentationType.SWAGGER_2)
        .apiInfo(apiInfo)
        .select()
        // see https://github.com/springfox/springfox/issues/631
        .apis(Predicates.not(
            RequestHandlerSelectors.basePackage("org.springframework.boot")))
        .build();
  }

  // https://github.com/springfox/springfox/issues/2155#issuecomment-352431668
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("swagger-ui.html")
        .addResourceLocations("classpath:/META-INF/resources/");

    registry.addResourceHandler("/webjars/**")
        .addResourceLocations("classpath:/META-INF/resources/webjars/");
  }
}
