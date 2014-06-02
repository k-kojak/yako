package hu.rgai.yako.eventlogger.rsa;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public enum RSAENCODING {
  INSTANCE;

  static private final BigInteger modulus = new BigInteger(
      "110019859073945242510877405778084481570732522148482645736391659659567807724977438079018362919244660194551718298987676235910517060152853035227390309704170871426389401061247064442968568023533222336262094702136640899204593996808788636121771395089960420082741676536557880104685444709096373538148003019718221305213");
  static private final BigInteger exponent = new BigInteger( "65537");

  static RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec( modulus, exponent);
  static RSAPublicKey publicKey = null;
  static Cipher cypher = null;
  static {

    try {
      KeyFactory fact = KeyFactory.getInstance( "RSA");
      publicKey = (RSAPublicKey) fact.generatePublic( publicKeySpec);
      cypher = Cipher.getInstance( "RSA/ECB/NoPadding");
      cypher.init( Cipher.ENCRYPT_MODE, publicKey);
    } catch (InvalidKeySpecException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchPaddingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private RSAENCODING() {
  }

  public String encodingString( String log) {

    try {
      return new String( cypher.doFinal( log.getBytes( "utf-8")));
    } catch (IllegalBlockSizeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (BadPaddingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }
}
