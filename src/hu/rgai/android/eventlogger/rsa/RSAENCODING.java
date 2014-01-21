package hu.rgai.android.eventlogger.rsa;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;

public enum RSAENCODING {
  INSTANCE;
  PublicKey publicKey = null;
  
  private RSAENCODING() {}
  
  public String encodingString(String log) throws InvalidKeyException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, ClassNotFoundException, BadPaddingException {
    /*KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    KeyPair myPair = kpg.generateKeyPair();

    Cipher c = Cipher.getInstance("RSA");
    c.init(Cipher.ENCRYPT_MODE, myPair.getPublic()); 
    
    byte[] encodedLog = c.doFinal(log.getBytes());
    
    return new String(encodedLog);*/
    return log;
  }
}
