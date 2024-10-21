/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.platform.httpClient;

import io.fabric8.openshift.api.model.OAuthAccessToken;
import io.fabric8.openshift.api.model.OAuthAccessTokenBuilder;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.api.model.OAuthClientBuilder;
import io.fabric8.openshift.api.model.User;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class OAuthToken {
    public String getToken(String redirectUrl) throws NoSuchAlgorithmException {
        // https://github.com/openshift/cluster-authentication-operator/blob/master/test/library/client.go#L35-L44
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String sha256Prefix = "sha256~";
        String randomToken = "nottoorandom%d".formatted(new Random().nextInt());
        byte[] hashed = digest.digest(randomToken.getBytes(StandardCharsets.UTF_8));
        String privateToken = sha256Prefix + randomToken;
        String publicToken = sha256Prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);

        User user = KubeResourceManager.getKubeClient().getOpenShiftClient().users().withName("kubeadmin").get();

        final String oauthClientName = "oauth-client";
        OAuthClient client = new OAuthClientBuilder()
                .withNewMetadata()
                .withName(oauthClientName)
                .endMetadata()
                .withSecret("the-secret-for-oauth-client")
                .withRedirectURIs("https://localhost")
                .withGrantMethod("auto")
                .withAccessTokenInactivityTimeoutSeconds(300)
                .build();
        KubeResourceManager.getInstance().createResourceWithoutWait(client);

        OAuthAccessToken token = new OAuthAccessTokenBuilder()
                .withNewMetadata()
                .withName(publicToken)
                .endMetadata()
                .withExpiresIn(86400L)
                .withScopes("user:full")
                .withRedirectURI(redirectUrl)
                .withClientName(oauthClientName)
                .withUserName(user.getMetadata().getName())
                .withUserUID(user.getMetadata().getUid())
                .build();
        KubeResourceManager.getInstance().createResourceWithWait(token);

        return privateToken;
    }
}
