package com.laura.payment_links.security;

import static com.laura.payment_links.security.Constants.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class JWTAuthtenticationConfig {

    public String getJWTToken(String username) {

        List<GrantedAuthority> grantedAuthorities = AuthorityUtils
                .commaSeparatedStringToAuthorityList("ROLE_USER");

        String token = Jwts
                .builder()
                // Establecer el ID del token
                .setId("campuscl")
                // Establecer el sujeto del token (en este caso, el nombre de usuario)
                .setSubject(username)
                // Agregar la lista de autoridades al token en forma de Claims
                .claim("authorities",
                        grantedAuthorities.stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toList()))
                // Establecer la fecha de emisión del token (actual)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // Establecer la fecha de expiración del token (actual + tiempo de expiración)
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_TIME))
                // Firmar el token con la clave secreta y el algoritmo de firma HS512
                .signWith(getSigningKey(SUPER_SECRET_KEY),  SignatureAlgorithm.HS512).compact();

        return "Bearer " + token;
    }

}
