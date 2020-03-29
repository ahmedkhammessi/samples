package com.manning.mss.ch10.sample01.config;

import java.io.File;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

@Configuration
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private Environment environment;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Bean
	public TokenEnhancer tokenEnhancer() {
		return new CustomJWTEnhancer();
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		String useJwt = environment.getProperty("spring.security.oauth.jwt");
		if (useJwt != null && "true".equalsIgnoreCase(useJwt.trim())) {

			TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
			enhancerChain.setTokenEnhancers(Arrays.asList(tokenEnhancer(), jwtConeverter()));
			endpoints.tokenStore(tokenStore()).accessTokenConverter(jwtConeverter()).tokenEnhancer(enhancerChain)
					.authenticationManager(authenticationManager);
			// endpoints.tokenStore(tokenStore()).tokenEnhancer(jwtConeverter())
			// .authenticationManager(authenticationManager);
		} else {
			endpoints.authenticationManager(authenticationManager);
		}
	}

	@Override
	public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
		security.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated()");
	}

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.inMemory().withClient("applicationid")
				.secret(passwordEncoder.encode("applicationsecret"))
				.scopes("foo", "bar")
				.authorizedGrantTypes("client_credentials", "password", "refresh_token")
				.accessTokenValiditySeconds(6000);
	}

	@Bean
	public TokenStore tokenStore() {
		String useJwt = environment.getProperty("spring.security.oauth.jwt");
		if (useJwt != null && "true".equalsIgnoreCase(useJwt.trim())) {
			return new JwtTokenStore(jwtConeverter());
		} else {
			return new InMemoryTokenStore();
		}
	}

	@Bean
	protected JwtAccessTokenConverter jwtConeverter() {
		String pwd = environment.getProperty("spring.security.oauth.jwt.keystore.password");
		String alias = environment.getProperty("spring.security.oauth.jwt.keystore.alias");
		String keystore = environment.getProperty("spring.security.oauth.jwt.keystore.name");

		KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new FileSystemResource(new File(keystore)),
				pwd.toCharArray());
		JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
		converter.setKeyPair(keyStoreKeyFactory.getKeyPair(alias));
		return converter;
	}
}
