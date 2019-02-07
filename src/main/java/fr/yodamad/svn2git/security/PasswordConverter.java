package fr.yodamad.svn2git.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Concrete implementation of the {@link AttributeConverter}
 * abstract class to encrypt/decrypt an entity attribute of type
 * {@link java.lang.String} <br/>
 * Note: This is the class where the {@literal @}Converter annotation is applied
 *
 * @author Sunit Katkar, sunitkatkar@gmail.com
 * @since ver 1.0 (Apr 2018)
 * @version 1.0 *
 */
@Component
@Configurable
@Converter(autoApply = false)
public class PasswordConverter implements AttributeConverter<String, String> {

    /** Spring env to retrieve secret. */
    private static Environment env;

    @Autowired
    public void initEnv(Environment en){
        PasswordConverter.env = en;
    }

    /** CipherMaker is needed to configure and create instance of Cipher */
    private CipherMaker cipherMaker;

    /**
     * Default constructor initializes with an instance of the
     * {@link CipherMaker} crypto class to get a {@link javax.crypto.Cipher}
     * instance
     */
    public PasswordConverter() {
        this(new CipherMaker());
    }

    /**
     * Constructor
     *
     * @param cipherMaker
     */
    public PasswordConverter(CipherMaker cipherMaker) {
        this.cipherMaker = cipherMaker;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.
     * Object)
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        String secret = env.getProperty("application.password.cipher");
        if (!StringUtils.isEmpty(secret) && !StringUtils.isEmpty(attribute)) {
            try {
                Cipher cipher = cipherMaker.configureAndGetInstance(Cipher.ENCRYPT_MODE, secret);
                return encryptData(cipher, attribute);
            } catch (NoSuchAlgorithmException
                | InvalidKeyException
                | InvalidAlgorithmParameterException
                | BadPaddingException
                | NoSuchPaddingException
                | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        }
        return convertEntityAttributeToString(attribute);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang.
     * Object)
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        String secret = env.getProperty("application.password.cipher");
        if (!StringUtils.isEmpty(secret) && !StringUtils.isEmpty(dbData)) {
            try {
                Cipher cipher = cipherMaker.configureAndGetInstance(Cipher.DECRYPT_MODE, secret);
                return decryptData(cipher, dbData);
            } catch (NoSuchAlgorithmException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | BadPaddingException
                | NoSuchPaddingException
                | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        }
        return convertStringToEntityAttribute(dbData);
    }

    private String convertStringToEntityAttribute(String dbData) {
        // the input is a string and output is a string
        return dbData;
    }

    private String convertEntityAttributeToString(String attribute) {
        // Here too the input is a string and output is a string
        return attribute;
    }

    /**
     * Helper method to encrypt data
     *
     * @param cipher
     * @param attribute
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private String encryptData(Cipher cipher, String attribute)
        throws IllegalBlockSizeException, BadPaddingException {
        byte[] bytesToEncrypt = convertEntityAttributeToString(attribute).getBytes();
        byte[] encryptedBytes = cipher.doFinal(bytesToEncrypt);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Helper method to decrypt data
     *
     * @param cipher
     * @param dbData
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    private String decryptData(Cipher cipher, String dbData)
        throws IllegalBlockSizeException, BadPaddingException {
        byte[] bytesToDecrypt = Base64.getDecoder().decode(dbData);
        byte[] decryptedBytes = cipher.doFinal(bytesToDecrypt);
        return convertStringToEntityAttribute(new String(decryptedBytes));
    }
}
