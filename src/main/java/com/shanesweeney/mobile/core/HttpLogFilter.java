package com.shanesweeney.mobile.core;

import org.openqa.selenium.remote.http.Contents;
import org.openqa.selenium.remote.http.Filter;
import org.openqa.selenium.remote.http.HttpHandler;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Logs every HTTP command the client sends to Appium and its response, at DEBUG on the
 * {@code appium.http} logger (target/appium-http.log). Pairs with the server's own [HTTP] lines in
 * appium-&lt;port&gt;.log when a command misbehaves.
 */
final class HttpLogFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger("appium.http");
    // Screenshots and page sources are base64/XML blobs; the start is enough to recognise them.
    private static final int MAX_BODY = 2_000;
    private static final Pattern ACCESS_KEY = Pattern.compile("(\"accessKey\"\\s*:\\s*\")[^\"]*");

    private final String server;

    HttpLogFilter(String server) {
        this.server = server;
    }

    @Override
    public HttpHandler apply(HttpHandler next) {
        return request -> {
            if (!log.isDebugEnabled()) {
                return next.execute(request);
            }
            log.debug("--> {} {}{} {}", request.getMethod(), server, request.getUri(), body(request));
            long start = System.nanoTime();
            HttpResponse response = next.execute(request);
            log.debug("<-- {} {} {}{} ({} ms) {}", response.getStatus(), request.getMethod(), server, request.getUri(),
                    (System.nanoTime() - start) / 1_000_000, body(response));
            return response;
        };
    }

    // Each reads the body and puts it back, since a message's content stream may only be readable once.
    private static String body(HttpRequest request) {
        byte[] bytes = Contents.bytes(request.getContent());
        request.setContent(Contents.bytes(bytes));
        return printable(bytes);
    }

    private static String body(HttpResponse response) {
        byte[] bytes = Contents.bytes(response.getContent());
        response.setContent(Contents.bytes(bytes));
        return printable(bytes);
    }

    private static String printable(byte[] bytes) {
        // Selenium pretty-prints request JSON; one line per message keeps the log greppable.
        String text = ACCESS_KEY.matcher(new String(bytes, StandardCharsets.UTF_8)).replaceAll("$1***")
                .replaceAll("\\s*\\R\\s*", " ");
        return text.length() <= MAX_BODY ? text : text.substring(0, MAX_BODY) + "... (" + text.length() + " chars)";
    }
}
