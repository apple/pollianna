/*
 * Copyright (c) 2023-2025 Apple Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apple.pollianna;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ParseArgumentsTest {

    @AfterEach
    public void tearDown() {
        Pollianna.stop();
    }

    @Test
    public void testInvalidArg() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Pollianna.start("interval:NaN"));
        assertEquals("could not parse interval value: NaN", exception.getMessage());
    }

    @Test
    public void testUnknownKeyword() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Pollianna.start("unknown:foo"));
        assertEquals("unknown keyword: unknown", exception.getMessage());
    }

    @Test
    public void testUnknownBean() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Pollianna.start("UnknownBean"));
        assertEquals("unknown Pollianna bean specified: UnknownBean", exception.getMessage());
    }

    @Test
    public void testEmptyArgs() {
        Pollianna.start();
        final String[] nullArgs = null;
        Pollianna.start(nullArgs);
        Pollianna.start("");
        Pollianna.start(" ");

        final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));
        Pollianna.start("interval:");
        assertTrue(errContent.toString().startsWith("could not read interval config"));
    }

    @Test
    public void testFileArg() {
        final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));
        Pollianna.start("file:");
        assertTrue(errContent.toString().startsWith("could not read file config"));

        final RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> Pollianna.start("file:none"));
        assertEquals("could not read file: none", runtimeException.getMessage());
    }

    @Test
    public void testMissingOTelArgs() {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> Pollianna.start("otel_endpoint:localhost"));
        assertEquals("OTel service name is required", runtimeException.getMessage());

        runtimeException = assertThrows(RuntimeException.class, () -> Pollianna.start("otel_endpoint:localhost", "otel_service_name:unittest"));
        assertEquals("OTel keystore path is required", runtimeException.getMessage());
    }

    @Test
    public void testOTelArgs() throws Exception {
        final File trustedRootFile = File.createTempFile("root", ".pem");
        try (final FileWriter writer = new FileWriter(trustedRootFile)) {
            // Test CA certificate.
            writer.write("-----BEGIN CERTIFICATE-----\n" +
                    "MIICHTCCAcOgAwIBAgIUa4fXissTPmulr3rb1JiNV7leTbMwCgYIKoZIzj0EAwIw\n" +
                    "ZDELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAkNBMRIwEAYDVQQHDAlDdXBlcnRpbm8x\n" +
                    "DjAMBgNVBAoMBUFwcGxlMQ4wDAYDVQQLDAVBcHBsZTEUMBIGA1UEAwwLZXhhbXBs\n" +
                    "ZS5jb20wHhcNMjUwMTI3MTAzNDMxWhcNMjgwMTI3MTAzNDMxWjBkMQswCQYDVQQG\n" +
                    "EwJVUzELMAkGA1UECAwCQ0ExEjAQBgNVBAcMCUN1cGVydGlubzEOMAwGA1UECgwF\n" +
                    "QXBwbGUxDjAMBgNVBAsMBUFwcGxlMRQwEgYDVQQDDAtleGFtcGxlLmNvbTBZMBMG\n" +
                    "ByqGSM49AgEGCCqGSM49AwEHA0IABP13zDtUxpx0aERb+V60I2BT3qnBUT90RlFA\n" +
                    "Czst7Cq4Yns4avBK2dj4nT3H8H1yZCp4CRkNfWoh9MV7SkfoZVGjUzBRMB0GA1Ud\n" +
                    "DgQWBBSJYcT0SoUCwqXHijvahW6uKew1KTAfBgNVHSMEGDAWgBSJYcT0SoUCwqXH\n" +
                    "ijvahW6uKew1KTAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMCA0gAMEUCIQD8\n" +
                    "ZiHFpYTMf+shVifbP4Q7OudeXPB2cJsV6kcBpQhYzwIgYKlqusia59iv0COBv/kL\n" +
                    "AbOFhqYHKO6Pj+Io8/0bhKs=\n" +
                    "-----END CERTIFICATE-----\n");
        }

        final char[] pwdArray = "password".toCharArray();
        final File passwordFile = File.createTempFile("keystore", ".password");
        try (final FileWriter writer = new FileWriter(passwordFile)) {
            writer.write(pwdArray);
        }

        final File keystoreFile = File.createTempFile("keystore", ".p12");
        final KeyStore ks = KeyStore.getInstance("pkcs12");
        ks.load(null, pwdArray);
        ks.store(Files.newOutputStream(keystoreFile.toPath()), pwdArray);

        Pollianna.start(
                "otel_endpoint:http://localhost:8008",
                "otel_service_name:unittest",
                "otel_client_keystore:" + keystoreFile.getAbsolutePath(),
                "otel_client_keystore_password:" + passwordFile.getAbsolutePath(),
                "otel_trusted_root:" + trustedRootFile.getAbsolutePath(),
                "otel_headers:Authentication=$TOKEN,X-TENANT=tenant-a",
                "otel_labels:foo=bar,host=$HOST"
        );
    }
}
