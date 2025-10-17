package com.C_platform.Member_woonkim.infrastructure.register_and_oauthClients;

import com.C_platform.Member_woonkim.application.port.OauthClientPort;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.exception.OauthErrorCode;
import com.C_platform.Member_woonkim.exception.OauthException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OauthClientRegister {
    private final Map<OAuthProvider, OauthClientPort> registry;

    public OauthClientRegister(List<OauthClientPort> clients) {
        this.registry = clients.stream()
                .collect(Collectors.toMap(OauthClientPort::getProvider, Function.identity()));
    }

    public OauthClientPort get(OAuthProvider provider) {
        var client = registry.get(provider);
        if (client == null)
            throw new OauthException(OauthErrorCode.C009);
        return client;
    }
}
