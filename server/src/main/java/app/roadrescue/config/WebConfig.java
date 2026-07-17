package app.roadrescue.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AuthInterceptor authInterceptor;
  private final String clientUrl;
  private final String clientUrls;

  public WebConfig(
      AuthInterceptor authInterceptor,
      @Value("${roadrescue.client-url}") String clientUrl,
      @Value("${roadrescue.client-urls}") String clientUrls
  ) {
    this.authInterceptor = authInterceptor;
    this.clientUrl = EnvFiles.value("CLIENT_URL", clientUrl);
    this.clientUrls = EnvFiles.value("CLIENT_URLS", clientUrls);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor);
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOriginPatterns(origins().toArray(String[]::new))
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }

  private List<String> origins() {
    var values = new ArrayList<String>();
    values.add(clientUrl);
    if (!clientUrls.isBlank()) {
      for (var item : clientUrls.split(",")) {
        if (!item.isBlank()) {
          values.add(item.trim());
        }
      }
    }
    values.add("https://*.vercel.app");
    values.add("https://*.onrender.com");
    return values;
  }
}
