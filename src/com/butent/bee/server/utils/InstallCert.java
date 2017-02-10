/*
 * Copyright 2006 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * Originally from:
 * http://blogs.sun.com/andreas/resource/InstallCert.java
 * Use:
 * java InstallCert hostname
 * Example:
 * % java InstallCert ecc.fedora.redhat.com
 */

package com.butent.bee.server.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Class used to add the server's certificate to the KeyStore
 * with your trusted certificates.
 */
public final class InstallCert {

  private static class SavingTrustManager implements X509TrustManager {

    private final X509TrustManager tm;
    private X509Certificate[] tmChain;

    SavingTrustManager(X509TrustManager tm) {
      this.tm = tm;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      /**
       * This change has been done due to the following resolution advised for Java 1.7+
       http://infposs.blogspot.kr/2013/06/installcert-and-java-7.html
       **/
      return new X509Certificate[0];
      //throw new UnsupportedOperationException();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
      this.tmChain = chain;
      tm.checkServerTrusted(chain, authType);
    }
  }

  private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

  private InstallCert() {
  }

  public static List<String> installCert(String... args) throws Exception {
    List<String> messages = new ArrayList<>();
    String host;
    int port;
    char[] passphrase;
    String store;
    boolean install;

    if (args.length > 1) {
      String[] c = args[0].split(":");
      host = c[0];
      port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
      store = args[1];
      install = (args.length > 2) && "install".equalsIgnoreCase(args[2]);
      passphrase = (args.length > 3 ? args[3] : "changeit").toCharArray();
    } else {
      messages.add("Usage: java InstallCert host[:port] pathToStore [check|install [passphrase]]");
      return messages;
    }
    File file = new File(store);
    File storeFile = file;

    if (!storeFile.isFile()) {
      char sep = File.separatorChar;
      File dir = new File(System.getProperty("java.home") + sep + "lib" + sep + "security");
      storeFile = new File(dir, "jssecacerts");

      if (!storeFile.isFile()) {
        storeFile = new File(dir, "cacerts");
      }
    }
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

    if (storeFile.isFile()) {
      messages.add("Loading KeyStore " + storeFile + "...");
      InputStream in = new FileInputStream(storeFile);
      ks.load(in, passphrase);
      in.close();
    } else {
      ks.load(null, passphrase);
    }
    SSLContext context = SSLContext.getInstance("TLS");
    TrustManagerFactory tmf =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);
    X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
    SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
    context.init(null, new TrustManager[] {tm}, null);
    SSLSocketFactory factory = context.getSocketFactory();

    messages.add("Opening connection to " + host + ":" + port + "...");
    SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
    socket.setSoTimeout(10000);
    try {
      messages.add("Starting SSL handshake...");
      socket.startHandshake();
      socket.close();
      messages.add("");
      messages.add("No errors, certificate is already trusted");
      return messages;
    } catch (SSLException e) {
      messages.add("");
      messages.add(e.getMessage());
    }
    X509Certificate[] chain = tm.tmChain;
    if (chain == null) {
      messages.add("Could not obtain server certificate chain");
      return messages;
    }
    messages.add("");
    messages.add("Server sent " + chain.length + " certificate(s):");
    messages.add("");
    MessageDigest sha1 = MessageDigest.getInstance("SHA1");
    MessageDigest md5 = MessageDigest.getInstance("MD5");

    for (int i = 0; i < chain.length; i++) {
      X509Certificate cert = chain[i];
      messages.add(" " + (i + 1) + " Subject " + cert.getSubjectDN());
      messages.add("   Issuer  " + cert.getIssuerDN());
      sha1.update(cert.getEncoded());
      messages.add("   sha1    " + toHexString(sha1.digest()));
      md5.update(cert.getEncoded());
      messages.add("   md5     " + toHexString(md5.digest()));
      messages.add("");
    }
    if (install) {
      for (int i = 0; i < chain.length; i++) {
        X509Certificate cert = chain[i];
        String alias = host + "-" + (i + 1);
        ks.setCertificateEntry(alias, cert);
      }
      OutputStream out = new FileOutputStream(file);
      ks.store(out, passphrase);
      out.close();

      messages.add("");
      messages.add("Added " + chain.length + " certificate(s) to keystore " + file);
    }
    return messages;
  }

  public static void main(String[] args) throws Exception {
    List<String> msgs = installCert(args);

    for (String msg : msgs) {
      System.out.println(msg);
    }
  }

  // CHECKSTYLE:OFF
  private static String toHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 3);
    for (int b : bytes) {
      b &= 0xff;
      sb.append(HEXDIGITS[b >> 4]);
      sb.append(HEXDIGITS[b & 15]);
      sb.append(' ');
    }
    return sb.toString();
  }
  // CHECKSTYLE:ON
}