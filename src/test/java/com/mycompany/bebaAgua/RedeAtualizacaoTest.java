package com.mycompany.bebaAgua;

import java.security.cert.*;
import java.util.List;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RedeAtualizacaoTest {
    private X509TrustManager validador(boolean aceita) {
        return new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
            public void checkClientTrusted(X509Certificate[] c,String t) throws CertificateException {checkServerTrusted(c,t);}
            public void checkServerTrusted(X509Certificate[] c,String t) throws CertificateException {
                if(!aceita)throw new CertificateException("não confiável");
            }
        };
    }
    @Test void rejeitaCertificadoNaoReconhecidoPorNenhumaFonte() {
        var manager=RedeAtualizacao.combinar(List.of(validador(false),validador(false)));
        assertThrows(CertificateException.class,()->manager.checkServerTrusted(new X509Certificate[0],"RSA"));
    }
    @Test void aceitaQuandoFonteAdicionalConfiaNoCertificado() throws Exception {
        RedeAtualizacao.combinar(List.of(validador(false),validador(true)))
            .checkServerTrusted(new X509Certificate[0],"RSA");
    }
}
