package jobworld.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import jobworld.services.UserServiceDefault;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http.authorizeRequests().
			antMatchers("/login").permitAll().
			antMatchers("/").permitAll().
			antMatchers("/company/**").hasAnyRole("COMPANY").
			antMatchers("/user/**").hasAnyRole("USER").
			antMatchers("/admin/**").hasAnyRole("ADMIN").
			and().formLogin().loginPage("/login").defaultSuccessUrl("/"). //pagina dove va quando la login ha successo
			failureUrl("/login?error=true").permitAll(). //pagina dove va se la login non ha successo
			and().logout().logoutSuccessUrl("/") //va a / dopo il logout. default path per logout � /logout
			.invalidateHttpSession(true).permitAll().
			and().csrf().disable();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return new UserServiceDefault();
	};

	@Autowired
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService()).passwordEncoder(this.passwordEncoder);
	}
}