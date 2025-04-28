package com.linkedout.auth.service;

import com.linkedout.auth.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String providerId = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        
        log.info("Provider ID: {}", providerId);
        log.info("Registration ID: {}", registrationId);
        
        OAuthAttributes attributes = extractAttributes(registrationId, providerId, oAuth2User.getAttributes());
        User user = convertToUser(attributes);
        
        log.info("User details: {}", user);
        
        return oAuth2User;
    }
    
    private OAuthAttributes extractAttributes(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if("google".equals(registrationId)) {
            return OAuthAttributes.builder()
                    .name((String) attributes.get("name"))
                    .email((String) attributes.get("email"))
                    .picture((String) attributes.get("picture"))
                    .attributes(attributes)
                    .nameAttributeKey(userNameAttributeName)
                    .build();
        }
        
        return null;
    }
    
    private User convertToUser(OAuthAttributes attributes) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .name(attributes.getName())
                .email(attributes.getEmail())
                .picture(attributes.getPicture())
                .roles(Collections.singleton("ROLE_USER"))
                .provider("google")
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    private static class OAuthAttributes {
        private Map<String, Object> attributes;
        private String nameAttributeKey;
        private String name;
        private String email;
        private String picture;
    }
}