package models


import org.joda.time.DateTime
import java.security.{KeyPair, KeyStore, PrivateKey, Signature}
import java.io.FileInputStream
import com.google.api.client.json.JsonFactory
import com.google.api.client.util.Base64
import com.google.api.client.util.StringUtils
import com.google.api.client.auth.jsontoken.{JsonWebToken, JsonWebSignature}
import com.google.api.client.json.jackson.JacksonFactory

/**
 * Created with IntelliJ IDEA.
 * User: administrator
 * Date: 1/16/13
 * Time: 1:08 PM
 * To change this template use File | Settings | File Templates.
 *
 * A JWT is composed of three parts: a header, a claim set, and a signature.
 * The header and claim set are JSON objects.
 *
 * These JSON objects are serialized to UTF-8 bytes, then encoded using the Base64url encoding.
 * This encoding provides resilience against encoding changes due to repeated encoding operations.
 *
 * The header, claim set, and signature are concatenated together with a ‘.’ character
 *
 */


/*
 *  Service Accounts rely on the RSA SHA256 algorithm and the JWT token format.
 */
object JWT {

  private val clientMail = "125812746657@developer.gserviceaccount.com"
  private val clientPass = "notasecret"
  private val scope = "https://www.googleapis.com/auth/calendar.readonly"
  private val access_tokenURL = "https://accounts.google.com/o/oauth2/token"

  var header = {
    val jws = new JsonWebSignature.Header
    jws.setAlgorithm("RS256")
    jws.setType("JWT")
  }

  /*
  The JWT claim set contains information about the JWT including the permissions being requested (scopes),
  the target of the token, the issuer,
  the time the token was issued,
  and the lifetime of the token.
  Like the JWT Header, the JWT claim set is a JSON object and is used in the calculation of the signature.

  * */


  var claimSet =  {
    val nowInSecondsSinze1970 = (System.currentTimeMillis / 1000)
    val expirationTimeInSeconds = 3600
    val exp = nowInSecondsSinze1970 + expirationTimeInSeconds
    val iat = nowInSecondsSinze1970
    val JWT = new JsonWebToken.Payload()
    JWT.setIssuer(clientMail)
      .setAudience(access_tokenURL)
      .setIssuedAtTimeSeconds(iat)
      .setExpirationTimeSeconds(exp)
    JWT.put("scope", scope);
    JWT
  }

  /**
   *
   * The signing algorithm in the JWT header must be used when computing the signature.
   * The only signing algorithm supported by the Google OAuth 2.0 Authorization Server is RSA using SHA-256 hashing algorithm.
   * This is expressed as ‘RS256’ in the ‘alg’ field in the JWT header.
   *
   * Sign the UTF-8 representation of the input using SHA256withRSA (also known as RSASSA-PKCS1-V1_5-SIGN with the SHA-256 hash function) with the private key obtained from the API console.
   * The output will be a byte array. The signature must then be Base64url encoded.
   *
   */

  def build:String = {

    val privateKey = getPrivateKey("conf/privatekey.p12", "privatekey", clientPass)
    val content = getContent
    val contentBytes = StringUtils.getBytesUtf8(content)
    val signer = Signature.getInstance("SHA256withRSA")
    signer.initSign(privateKey)
    signer.update(contentBytes)
    content + "." + Base64.encodeBase64URLSafeString(signer.sign())
  }

  def getPrivateKey(keyFile: String, keyAlias:String, keyPassword:String): PrivateKey = {
    val keyStore = KeyStore.getInstance("PKCS12")
    keyStore.load(new FileInputStream(keyFile), keyPassword.toCharArray)
    keyStore.getKey(keyAlias, keyPassword.toCharArray).asInstanceOf[PrivateKey]
  }

  def getContent: String = {
    val jsonFactory = new JacksonFactory()
    Base64.encodeBase64URLSafeString(jsonFactory.toByteArray(header)) + "." + Base64.encodeBase64URLSafeString(jsonFactory.toByteArray(claimSet));
  }
}
