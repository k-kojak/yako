package hu.rgai.yako.eventlogger.rsa;

import hu.rgai.yako.eventlogger.EventLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.util.Base64;

import hu.rgai.android.test.R;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

public enum RSAENCODING {
  INSTANCE;

  PublicKey publicKey = null;
  PrivateKey privateKey = null;
  Cipher cypherToEncrypt = null;
  Cipher cypherToDecrypt = null;
  final String ALGORITHM = "RSA/ECB/PKCS1Padding";

  private RSAENCODING() {
    ObjectInputStream inputStream = null;

    // Encrypt the string using the public key
    
    Context context = EventLogger.INSTANCE.getContext();
    try {
      inputStream = new ObjectInputStream(context.getResources().openRawResource(R.raw.publickey));
      publicKey = (PublicKey) inputStream.readObject();
      inputStream = new ObjectInputStream(context.getResources().openRawResource(R.raw.server_private));
      privateKey = (PrivateKey) inputStream.readObject();
      cypherToEncrypt = Cipher.getInstance(ALGORITHM );
      cypherToEncrypt.init(Cipher.ENCRYPT_MODE, publicKey);     
      cypherToDecrypt = Cipher.getInstance(ALGORITHM );
      cypherToDecrypt.init(Cipher.DECRYPT_MODE, privateKey);
    } catch (NotFoundException | IOException | ClassNotFoundException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public String encryptString( String log) {

    try {
      return Base64.encodeBytes(cypherToDecrypt.doFinal( log.getBytes("utf-8")));
    } catch (IllegalBlockSizeException e) {
      Log.d("willrgai", "", e);
    } catch (BadPaddingException e) {
      Log.d("willrgai", "", e);
    } catch (UnsupportedEncodingException e) {
      Log.d("willrgai", "", e);
    }
    return null;
  }
  
  public String decryptString( String log) {

    try {
      return new String(cypherToDecrypt.doFinal(  Base64.decode(log.replaceAll("\\\\n", ""))));
    } catch (IllegalBlockSizeException e) {
      Log.d("willrgai", "", e);
    } catch (BadPaddingException e) {
      Log.d("willrgai", "", e);
    }
    return null;
  }
}
