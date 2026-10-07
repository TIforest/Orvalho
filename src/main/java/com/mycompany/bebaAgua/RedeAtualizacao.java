package com.mycompany.bebaAgua;

import java.security.*;
import java.security.cert.*;
import java.util.*;
import javax.net.ssl.*;

/** Confia nas raízes do Java e nas raízes administradas no Windows, sem ignorar TLS. */
final class RedeAtualizacao {
    private static SSLSocketFactory fabrica;

    static synchronized SSLSocketFactory fabrica() throws GeneralSecurityException {
        if (fabrica != null) return fabrica;
        List<X509TrustManager> validadores = new ArrayList<>();
        adicionar(validadores, null);
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            for (String tipo : List.of("Windows-ROOT", "Windows-ROOT-LOCALMACHINE")) {
                try {
                    KeyStore certificados = KeyStore.getInstance(tipo);
                    certificados.load(null, null);
                    adicionar(validadores, certificados);
                } catch (Exception erro) {
                    DiagnosticoAtualizacao.registrar("Não foi possível consultar " + tipo + ": " + erro);
                }
            }
        }
        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(null, new TrustManager[]{combinar(validadores)}, null);
        fabrica = contexto.getSocketFactory();
        return fabrica;
    }

    private static void adicionar(List<X509TrustManager> lista, KeyStore certificados) throws GeneralSecurityException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(certificados);
        for (TrustManager manager : factory.getTrustManagers())
            if (manager instanceof X509TrustManager x509) lista.add(x509);
    }

    static X509TrustManager combinar(List<X509TrustManager> lista) {
        return new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return lista.stream().flatMap(v -> Arrays.stream(v.getAcceptedIssuers())).toArray(X509Certificate[]::new);
            }
            public void checkClientTrusted(X509Certificate[] cadeia, String tipo) throws CertificateException {
                lista.getFirst().checkClientTrusted(cadeia, tipo);
            }
            public void checkServerTrusted(X509Certificate[] cadeia, String tipo) throws CertificateException {
                CertificateException falha = new CertificateException("Certificado não confiável no Java nem no Windows");
                for (X509TrustManager validador : lista) {
                    try { validador.checkServerTrusted(cadeia, tipo); return; }
                    catch (CertificateException e) { falha.addSuppressed(e); }
                }
                throw falha;
            }
        };
    }
}
