package br.unesp.fct.evcomp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração central do Spring Security.
 * Define as regras de acesso (quem pode acessar o quê), substitui o antigo AuthInterceptor,
 * configura CORS e desabilita CSRF (desnecessário com JWT stateless).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Desabilita CSRF — seguro porque usamos JWT (sem cookies de sessão)
            .csrf(csrf -> csrf.disable())

            // Configura CORS (substituindo o WebMvcConfig)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Sessões stateless — cada requisição se autentica via JWT
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Regras de autorização
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos (sem autenticação)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/cadastro/**").permitAll()
                .requestMatchers("/api/redefinicao-senha/**").permitAll()
                .requestMatchers("/error").permitAll()

                // Relatórios: somente ADMIN
                .requestMatchers("/api/relatorios/**").hasRole("ADMIN")

                // Eventos: Listar participantes apenas ADMIN, outros GETs livres para autenticados, POST/PUT/DELETE somente ADMIN
                .requestMatchers(HttpMethod.GET, "/api/eventos/*/participantes").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/eventos/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/eventos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/eventos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/eventos/**").hasRole("ADMIN")

                // Atividades: GET é livre para autenticados, POST/PUT/DELETE somente ADMIN
                .requestMatchers(HttpMethod.GET, "/api/atividades/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/atividades/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/atividades/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/atividades/**").hasRole("ADMIN")
                
                // Participantes: Visualização apenas para ADMIN
                .requestMatchers(HttpMethod.GET, "/api/participantes/**").hasRole("ADMIN")

                // Pagamentos: fila de conferência e avaliação de comprovante somente ADMIN.
                // O restante (consulta e upload do próprio comprovante) cai em authenticated()
                // e tem a checagem de dono feita dentro do PagamentoController.
                .requestMatchers(HttpMethod.GET, "/api/pagamentos/pendentes").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/pagamentos/evento/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/pagamentos/*/status").hasRole("ADMIN")

                // Todo o resto: precisa estar autenticado
                .anyRequest().authenticated()
            )

            // Adiciona o filtro JWT ANTES do filtro padrão do Spring Security
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuração de CORS centralizada no Spring Security.
     * Substitui a configuração que estava no WebMvcConfig.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "https://secompp.com.br",
            "https://evcomp.secompp.com.br",
            "https://*.secompp.com.br"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
