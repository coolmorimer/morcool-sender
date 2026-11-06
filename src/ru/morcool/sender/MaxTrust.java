package ru.morcool.sender;
import android.content.Context;import java.io.InputStream;import java.security.*;import java.security.cert.*;import javax.net.ssl.*;

/** Scoped MAX API trust. Default hostname verification is always kept. No global/system trust-store changes. */
public final class MaxTrust {
 private static SSLSocketFactory factory;
 public static synchronized SSLSocketFactory socketFactory(Context c)throws Exception{
  if(factory!=null)return factory;
  TrustManagerFactory system=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());system.init((KeyStore)null);X509TrustManager normal=(X509TrustManager)system.getTrustManagers()[0];
  CertificateFactory certs=CertificateFactory.getInstance("X.509");X509Certificate cert;try(InputStream in=c.getResources().openRawResource(R.raw.max_root)){cert=(X509Certificate)certs.generateCertificate(in);}cert.checkValidity();
  KeyStore extra=KeyStore.getInstance(KeyStore.getDefaultType());extra.load(null,null);extra.setCertificateEntry("max-root",cert);TrustManagerFactory special=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());special.init(extra);X509TrustManager max=(X509TrustManager)special.getTrustManagers()[0];
  X509TrustManager scoped=new X509TrustManager(){public void checkClientTrusted(X509Certificate[] chain,String auth)throws CertificateException{normal.checkClientTrusted(chain,auth);}public void checkServerTrusted(X509Certificate[] chain,String auth)throws CertificateException{try{normal.checkServerTrusted(chain,auth);}catch(CertificateException e){max.checkServerTrusted(chain,auth);}}public X509Certificate[] getAcceptedIssuers(){X509Certificate[] a=normal.getAcceptedIssuers(),b=max.getAcceptedIssuers(),out=new X509Certificate[a.length+b.length];System.arraycopy(a,0,out,0,a.length);System.arraycopy(b,0,out,a.length,b.length);return out;}};
  SSLContext ssl=SSLContext.getInstance("TLS");ssl.init(null,new TrustManager[]{scoped},null);factory=ssl.getSocketFactory();return factory;
 }
}
