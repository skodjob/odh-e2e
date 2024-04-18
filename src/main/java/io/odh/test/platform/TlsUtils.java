/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.odh.test.platform;

import io.fabric8.kubernetes.api.model.Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TlsUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TlsUtils.class);

    public static SSLContext getSSLContextFromSecret(Secret signingKey) throws Exception {
        String caSecret = signingKey.getData().get("tls.crt");
        return createSslContext(caSecret);
    }

    private static SSLContext createSslContext(String base64EncodedPems) throws Exception {
        Base64.Decoder decoder = Base64.getMimeDecoder();
        String pem = new String(decoder.decode(base64EncodedPems));
        Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN ([^-]+)---*$([^-]+)^---*END[^-]+-+$");
        Matcher m = parse.matcher(pem);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        List<Certificate> certList = new ArrayList<>();

        int start = 0;
        while (m.find(start)) {
            String type = m.group(1);
            String base64Data = m.group(2);
            byte[] data = decoder.decode(base64Data);
            start += m.group(0).length();
            type = type.toUpperCase(Locale.ENGLISH);
            if (type.contains("CERTIFICATE")) {
                Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(data));
                certList.add(cert);
            } else {
                LOGGER.error("Unsupported type: {}", type);
            }
        }

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);

        int count = 0;
        for (Certificate cert : certList) {
            trustStore.setCertificateEntry("cert" + count, cert);
            count++;
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext;
    }
}
