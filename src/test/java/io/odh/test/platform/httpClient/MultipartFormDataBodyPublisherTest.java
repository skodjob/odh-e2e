/*
 * Copyright Skodjob authors.
 * Copyright (c) 2022 yusuke suzuki
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.odh.test.platform.httpClient;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class MultipartFormDataBodyPublisherTest {
    @Test
    public void testStringPart() throws IOException {
        var part = new StringPart("key", "val", Charset.forName("utf8"));
        assertEquals(part.name(), "key");

        var buf = new byte[3];
        var input = part.open();
        assertEquals(3, input.read(ByteBuffer.wrap(buf)));
        assertArrayEquals("val".getBytes(), buf);

        assertEquals(Optional.empty(), part.filename());
        assertEquals(Optional.empty(), part.contentType());
    }

    @Test
    public void testFilePart() throws Exception {
        var part = new FilePart("key", Path.of("pom.xml"));
        assertEquals(part.name(), "key");

        var buf = new byte[1024 * 8];
        var actualDigest = MessageDigest.getInstance("SHA-256");
        var input = new DigestInputStream(Channels.newInputStream(part.open()), actualDigest);
        while (input.read(buf) != -1) {
            // nop
        }

        var expectDigest = MessageDigest.getInstance("SHA-256");
        var input2 = new DigestInputStream(Files.newInputStream(Path.of("pom.xml")), expectDigest);
        while (input2.read(buf) != -1) {
            // nop
        }
        assertArrayEquals(expectDigest.digest(), actualDigest.digest());

        assertEquals(Optional.of("pom.xml"), part.filename());
        assertEquals(Optional.of("application/octet-stream"), part.contentType());
    }

    @Test
    public void testStreamPart() throws Exception {
        var part = new StreamPart("key", "fname",
                () -> Channels.newChannel(new ByteArrayInputStream("hello, world!".getBytes())));
        assertEquals(part.name(), "key");

        var buf = new byte[13];
        var b = ByteBuffer.wrap(buf);
        var input = part.open();
        while (input.read(b) != -1 && b.hasRemaining()) {
            // nop
        }
        assertArrayEquals("hello, world!".getBytes(), buf);

        assertEquals(Optional.of("fname"), part.filename());
        assertEquals(Optional.of("application/octet-stream"), part.contentType());
    }

    @Test
    public void testMultipartFormDataChannel() throws Exception {
        var channel = new MultipartFormDataChannel("----boundary",
                List.<Part>of(new StringPart("key", "value", StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        assertTrue(channel.isOpen());
        var content = new StringBuilder();
        try (channel) {
            var r = Channels.newReader(channel, StandardCharsets.UTF_8);
            var buf = new char[1024 * 8];
            int n;
            while ((n = r.read(buf)) != -1) {
                content.append(buf, 0, n);
            }
        }
        assertFalse(channel.isOpen());

        var expect = "------boundary\r\n" + "Content-Disposition: form-data; name=\"key\"\r\n" + "\r\n" + "value\r\n"
                + "------boundary--\r\n";
        assertEquals(expect, content.toString());
    }

    @Test
    public void testMultipartFormDataChannelException() {
        var exception = new IOException();
        var channel = new MultipartFormDataChannel("----boundary",
                List.<Part>of(new StreamPart("key", "fname", () -> new ReadableByteChannel() {
                    @Override
                    public void close() {
                    }

                    @Override
                    public boolean isOpen() {
                        return true;
                    }

                    @Override
                    public int read(ByteBuffer buf) throws IOException {
                        throw exception;
                    }
                })), StandardCharsets.UTF_8);
        try (channel) {
            while (channel.read(ByteBuffer.allocate(1)) != -1) {
                // nop
            }
            fail(); // unreachable
        } catch (IOException e) {
            assertEquals(exception, e);
        }
    }

    @Test
    public void testMultipartFormData() throws Exception {
        var httpd = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        new Thread(() -> httpd.start()).start();
        try {
            var publisher = new MultipartFormDataBodyPublisher().add("key", "value").addFile("f1", Path.of("pom.xml"))
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

    // FIXME remove
    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_ENABLE_WITH_GO", matches = ".+")
    public void testWithGoServer() throws Exception {
        var proc = new ProcessBuilder("go", "run", "main.go").redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        // signal via stdio
        proc.getInputStream().read();

        try {
            var publisher = new MultipartFormDataBodyPublisher().add("key", "value")
                    .addFile("f1", Path.of(".gitignore")).addFile("f2", Path.of(".gitignore"), "text/plain")
                    .addStream("f3", "fname", () -> new ByteArrayInputStream("a".getBytes()))
                    .addStream("f4", "fname", () -> new ByteArrayInputStream("b".getBytes()), "text/plain")
                    .addChannel("f5", "fname", () -> Channels.newChannel(new ByteArrayInputStream("c".getBytes())))
                    .addChannel("f6", "fname", () -> Channels.newChannel(new ByteArrayInputStream("d".getBytes())),
                            "text/plain");
            var client = HttpClient.newHttpClient();
            var request = HttpRequest.newBuilder(new URI("http", null, "localhost", 8080, "/", null, null))
                    .header("Content-Type", publisher.contentType()).POST(publisher).build();
            var response = client.send(request, BodyHandlers.discarding());
            assertEquals(response.statusCode(), 200);
        } finally {
            try (var o = proc.getOutputStream()) {
                // signal stop via stdio
                o.write('a');
            } catch (Exception e) {
                e.printStackTrace();
            }
            proc.onExit().get();
        }
    }
}
