package com.oussema.cloud.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.core.env.Environment;

/**
 * JPA Attribute Converter for encrypting and decrypting String attributes. This converter uses the
 * Jasypt library for encryption and decryption operations. It is configured with an environment
 * variable for the encryption password.
 * <p>
 * Example:
 * <pre>
 * &#64;Column(name = "cin_number", nullable = false, unique = true)
 * &#64;Convert(converter = StringCryptoConverter.class)
 * private String number;
 * </pre>
 * This will automatically encrypt the `number` field when saving to the database and decrypt it
 */
@Converter
public class StringCryptoConverter implements AttributeConverter<String, String> {

    // Jasypt StringEncryptor for performing encryption and decryption
    private final StandardPBEStringEncryptor encryptor;

    /**
     * Constructor for StringCryptoConverter.
     *
     * @param environment The Spring Environment used to access properties.
     */
    public StringCryptoConverter(Environment environment) {
        // Get the encryption password from the environment
        String encryptionPassword = environment.getProperty("jasypt.encryptor.password");

        if (encryptionPassword == null || encryptionPassword.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Encryption password not found. Please set the 'jasypt.encryptor.password' property.");
        }

        // Initialize the encryptor with the encryption password from the environment
        this.encryptor = new StandardPBEStringEncryptor();
        this.encryptor.setPassword(encryptionPassword);
        this.encryptor.setAlgorithm("PBEWithMD5AndDES");
    }

    /**
     * Converts the attribute value to the encrypted form.
     *
     * @param attribute The original attribute value to be encrypted.
     * @return The encrypted form of the attribute.
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptor.encrypt(attribute);
    }

    /**
     * Converts the encrypted database value to its decrypted form.
     *
     * @param dbData The encrypted value stored in the database.
     * @return The decrypted form of the database value.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptor.decrypt(dbData);
    }
}
