package com.example.springsecurityapplication.config;

import com.example.springsecurityapplication.services.PersonDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final PersonDetailsService personDetailsService;

    @Bean
    public PasswordEncoder getPasswordEncode() {
        // return NoOpPasswordEncoder.getInstance(); //Пароли в открытом доступе
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Конфигурируем работу Spring Security
        // .csrf().disable() //отключаем защиту от межсайтовой подделки запросов
        http
            .authorizeHttpRequests() // указываем, что все страницы д.б. защищены аутентификацией указываем, что не аутентифицированные пользователи могут зайти на страницу аутентификации и на объект ошибки с помощью permitAll указываем, что не аутентифицированные пользователи могут заходить на перечисленные страницы
            .requestMatchers("/","/authentication", "/registration", "/error", "/resources/**", "/static/**", "/css/**", "/js/**", "/img/**", "/product", "/product/info/{id}", "/product/search").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN") // указываем на то, что страница /admin доступна пользователю с ролью ADMIN
            // .requestMatchers("/authentication", "/registration", "/errors", "/resources/**", "/static/**").permitAll()
            // указываем, что для всех остальных страниц необходимо вызывать метод authenticated(), который открывает форму аутентификации
            // .anyRequest().authenticated()
            .anyRequest().hasAnyRole("USER", "ADMIN")
            .and() // указываем, что дальше настраивается аутентификация и соединяем ее с настройкой доступа
            .formLogin(login -> login.loginPage("/authentication") // указываем какой url запрос будет отправляться при заходе на защищенные страницы
                    .loginProcessingUrl("/process_login") // указываем на какой адрес будут отправляться данные с формы. Нам уже не нужно будет создавать метод в контроллере и обрабатывать данные с формы. Мы задали url, который используется по умолчанию для обработки формы аутентификации посредством Spring Security. Spring Security будет ждать объект с формы аутентификации и затем сверять логин и пароль с данными в БД.
                    .defaultSuccessUrl("/person_account", true) // указываем на какой url необходимо отправить пользователя после успешной аутентификации. Вторым аргументом указывается true чтобы перенаправление шло в любом случае после успешной аутентификации
                    .failureUrl("/authentication?error")) // указываем куда необходимо перенаправить пользователя при проваленной аутентификации. В запрос будет передан объект error, который будет проверяться на форме и при наличии данного объекта в запросе выводится сообщение "Неправильный логин или пароль"
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/authentication"));
        return http.build();
    }

    @Autowired
    public SecurityConfig(PersonDetailsService personDetailsService) {
        this.personDetailsService = personDetailsService;
    }

    // private final AuthenticationProvider authenticationProvider;

    // public SecurityConfig(AuthenticationProvider authenticationProvider) {
    // this.authenticationProvider = authenticationProvider;
    // }

    protected void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {

        // authenticationManagerBuilder.authenticationProvider(authenticationProvider);
        authenticationManagerBuilder.userDetailsService(personDetailsService)
                .passwordEncoder(getPasswordEncode());
    }
}
