/*
 * Copyright Skodjob authors.
 * Copyright (c) 2022 yusuke suzuki
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.unit;

import com.sun.net.httpserver.HttpServer;
import io.odh.test.platform.httpClient.MultipartFormDataBodyPublisher;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

public class MultipartFormDataBodyPublisherTests {
    @Test
    public void testStringPart() throws IOException {
        var publisher = new MultipartFormDataBodyPublisher()
                .add("key", "val");
        var str = suckStringFromPublisher(publisher);

        assertThat(str, Matchers.matchesRegex(
                getMultipartRegex("Content-Disposition: form-data; name=\"key\"\r\n\r\nval")));
    }

    @Test
    public void testFilePart() throws Exception {
        Path tempFile = Files.createTempFile(null, null);
        tempFile.toFile().deleteOnExit();
        Files.writeString(tempFile, "Hello World\n");

        var publisher = new MultipartFormDataBodyPublisher()
                .addFile("key", tempFile);
        var str = suckStringFromPublisher(publisher);

        assertThat(str, Matchers.matchesRegex(
                getMultipartRegex("Content-Disposition: form-data; name=\"key\"; filename=\"%s\"\r\nContent-Type: application/octet-stream\r\n\r\nHello World\n".formatted(tempFile.getFileName()))));
    }

    @Test
    public void testStreamPart() throws Exception {
        var publisher = new MultipartFormDataBodyPublisher()
                .addStream("key", "fname",
                        () -> new ByteArrayInputStream("hello, world!".getBytes()));
        var str = suckStringFromPublisher(publisher);

        assertThat(str, Matchers.matchesRegex(
                getMultipartRegex("Content-Disposition: form-data; name=\"key\"; filename=\"fname\"\r\nContent-Type: application/octet-stream\r\n\r\nhello, world!")));
    }

    @Test
    public void testMultipartFormDataChannel() throws Exception {
        var publisher = new MultipartFormDataBodyPublisher()
                .add("key1", "val1")
                .add("key2", "val2");
        var str = suckStringFromPublisher(publisher);

        assertThat(str, Matchers.matchesRegex(
                getMultipartRegex(
                        "Content-Disposition: form-data; name=\"key1\"\r\n\r\nval1",
                        "Content-Disposition: form-data; name=\"key2\"\r\n\r\nval2")));
    }

    @Test
    public void testMultipartFormDataChannelException() {
        var ioException = new IOException();

        var publisher = new MultipartFormDataBodyPublisher()
                .addStream("key", "fname", () -> new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw ioException;
                    }

                    @Override
                    public void close() {
                    }
                });

        var exception = Assertions.assertThrows(RuntimeException.class, () -> suckStringFromPublisher(publisher));
        Assertions.assertSame(ioException, exception.getCause().getCause());
    }

    @Test
    public void testMultipartFormData() throws Exception {
        var httpd = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        new Thread(() -> httpd.start()).start();
        try {
            var publisher = new MultipartFormDataBodyPublisher()
                    .add("key", "value")
                    .addFile("f1", Path.of("pom.xml"))
                    .addFile("f2", Path.of("pom.xml"), "application/xml")
                    .addStream("f3", "fname", () -> new ByteArrayInputStream("".getBytes()))
                    .addStream("f4", "fname", () -> new ByteArrayInputStream("".getBytes()), "application/xml")
                    .addChannel("f5", "fname", () -> Channels.newChannel(new ByteArrayInputStream("".getBytes())))
                    .addChannel("f6", "fname", () -> Channels.newChannel(new ByteArrayInputStream("".getBytes())),
                            "application/xml");
            var client = HttpClient.newHttpClient();
            var request = HttpRequest
                    .newBuilder(new URI("http", null, "localhost", httpd.getAddress().getPort(), "/", null, null))
                    .header("Content-Type", publisher.contentType()).POST(publisher).build();
            client.send(request, BodyHandlers.discarding());

        } finally {
            httpd.stop(0);
        }
    }

    String getMultipartRegex(String... bodies) {
        //language=RegExp
        var regexBuffer = new StringBuilder();
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
        var result = new StringBuilder();
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
