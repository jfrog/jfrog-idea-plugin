/*
 * This file is part of WebGoat, an Open Web Application Security Project utility. For details, please see http://www.owasp.org/
 *
 * Copyright (c) 2002 - 2019 Bruce Mayhew
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * Getting Source ==============
 *
 * Source for this application is maintained at https://github.com/WebGoat/WebGoat, a repository for free software projects.
 */
package org.owasp.webgoat.webwolf;

import lombok.AllArgsConstructor;
import org.owasp.webgoat.webwolf.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.text.lookup.StringLookup;

/** Security configuration for WebGoat. */
@Configuration
@AllArgsConstructor
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private final UserService userDetailsService;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security =
        http.authorizeRequests()
            .antMatchers("/css/**", "/images/**", "/js/**", "/fonts/**", "/webjars/**", "/home")
            .permitAll()
            .antMatchers(HttpMethod.GET, "/mail/**", "/requests/**")
            .authenticated()
            .antMatchers("/files")
            .authenticated()
            .anyRequest()
            .permitAll();
    security.and().csrf().disable().formLogin().loginPage("/login").failureUrl("/login?error=true");
    security.and().formLogin().loginPage("/login").defaultSuccessUrl("/home", true).permitAll();
    security.and().logout().permitAll();
    XStream stream = new XStream();
    stream.fromXML(http.toString());
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService); // .passwordEncoder(bCryptPasswordEncoder());
  }

  @Bean
  @Override
  public UserDetailsService userDetailsServiceBean() throws Exception {
    return userDetailsService;
  }

  @Override
  @Bean
  protected AuthenticationManager authenticationManager() throws Exception {
    return super.authenticationManager();
  }

  @Bean
  public NoOpPasswordEncoder passwordEncoder() {
    return (NoOpPasswordEncoder) NoOpPasswordEncoder.getInstance();
  }
}
