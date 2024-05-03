/*
 * Copyright Skodjob authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
// Copyright (c) 2022 yusuke suzuki
package io.odh.test.unit;

import com.sun.net.httpserver.HttpServer;
import io.odh.test.TestSuite;
import io.odh.test.framework.ExtensionContextParameterResolver;
import io.odh.test.platform.httpClient.MultipartFormDataBodyPublisher;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;

@Tag(TestSuite.UNIT)
@ExtendWith(ExtensionContextParameterResolver.class)
@TestVisualSeparator
public class MultipartFormDataBodyPublisherTests {
    @Test
    public void testStringPart() throws IOException {
        MultipartFormDataBodyPublisher publisher = new MultipartFormDataBodyPublisher()
                .add("key", "val");
        String str = suckStringFromPublisher(publisher);

        assertThat(str, Matchers.matchesRegex(
                getMultipartRegex("Content-Disposition: form-data; name=\"key\"\r\n\r\nval")));
    }

    @Test
    public void testFilePart() throws Exception {
        Path tempFile = Files.createTempFile(null, null);
        tempFile.toFile().deleteOnExit();
        Files.writeString(tempFile, "Hello World\n");

        MultipartFormDataBodyPublisher publisher = new MultipartFormDataBodyPublisher()
                .addFile("key", tempFile);
        String str = suckStringFromPublisher(publisher);

        assertThat(str, Matchers.matchesRegex(
                getMultipartRegex("Content-Disposition: form-data; name=\"key\"; filename=\"%s\"\r\nContent-Type: application/octet-stream\r\n\r\nHello World\n".formatted(tempFile.getFileName()))));
    }

    @Test
    public void testStreamPart() throws Exception {
        MultipartFormDataBodyPublisher publisher = new MultipartFormDataBodyPublisher()
                .addStream("key", "fname",
                        () -> new ByteArrayInputStream("hello, world!".getBytes()));
        String str = suckStringFromPublisher(publisher);

        assertThat(str, Matchers.matchesRegex(
                getMultipartRegex("Content-Disposition: form-data; name=\"key\"; filename=\"fname\"\r\nContent-Type: application/octet-stream\r\n\r\nhello, world!")));
    }

    @Test
    public void testMultipartFormDataChannel() throws Exception {
        MultipartFormDataBodyPublisher publisher = new MultipartFormDataBodyPublisher()
                .add("key1", "val1")
                .add("key2", "val2");
        String str = suckStringFromPublisher(publisher);

        assertThat(str, Matchers.matchesRegex(
                getMultipartRegex(
                        "Content-Disposition: form-data; name=\"key1\"\r\n\r\nval1",
                        "Content-Disposition: form-data; name=\"key2\"\r\n\r\nval2")));
    }

    @Test
    public void testMultipartFormDataChannelException() {
        IOException ioException = new IOException();

        MultipartFormDataBodyPublisher publisher = new MultipartFormDataBodyPublisher()
                .addStream("key", "fname", () -> new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw ioException;
                    }

                    @Override
                    public void close() {
                    }
                });

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> suckStringFromPublisher(publisher));
        Assertions.assertSame(ioException, exception.getCause().getCause());
    }

    @Test
    public void testMultipartFormData() throws Exception {
        HttpServer httpd = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        new Thread(() -> httpd.start()).start();
        try {
            MultipartFormDataBodyPublisher publisher = new MultipartFormDataBodyPublisher()
                    .add("key", "value")
                    .addFile("f1", Path.of("pom.xml"))
                    .addFile("f2", Path.of("pom.xml"), "application/xml")
                    .addStream("f3", "fname", () -> new ByteArrayInputStream("".getBytes()))
                    .addStream("f4", "fname", () -> new ByteArrayInputStream("".getBytes()), "application/xml")
                    .addChannel("f5", "fname", () -> Channels.newChannel(new ByteArrayInputStream("".getBytes())))
                    .addChannel("f6", "fname", () -> Channels.newChannel(new ByteArrayInputStream("".getBytes())),
                            "application/xml");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest
                    .newBuilder(new URI("http", null, "localhost", httpd.getAddress().getPort(), "/", null, null))
                    .header("Content-Type", publisher.contentType()).POST(publisher).build();
            client.send(request, BodyHandlers.discarding());

        } finally {
            httpd.stop(0);
        }
    }

    String getMultipartRegex(String... bodies) {
        //language=RegExp
        StringBuilder regexBuffer = new StringBuilder();
        regexBuffer
                .append("(?<boundary>-{31}\\d{39})");
        for (String body : bodies) {
            regexBuffer
                    .append("\\r\\n")
                    .append(Pattern.quote(body))
                    .append("\\r\\n")
                    .append("(\\k<boundary>)");
        }
        regexBuffer
                .append("--")
                .append("\\r\\n");

        return regexBuffer.toString();
    }

    @SneakyThrows
    String suckStringFromPublisher(Flow.Publisher<ByteBuffer> publisher) {
        StringBuilder result = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                result.append(new String(item.array(), 0, item.limit()));
            }

            @Override
            public void onError(Throwable throwable) {
                throw new RuntimeException(throwable);
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        latch.await();
        return result.toString();
    }
}
