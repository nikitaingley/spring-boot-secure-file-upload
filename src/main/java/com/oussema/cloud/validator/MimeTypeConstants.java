package com.oussema.cloud.validator;

import org.springframework.http.MediaType;

import java.util.List;

public class MimeTypeConstants {


    // Private constructor to prevent instantiation
    private MimeTypeConstants() {
    }


    public static final String APPLICATION_PDF = MediaType.APPLICATION_PDF_VALUE;
    public static final String IMAGE_JPEG = MediaType.IMAGE_JPEG_VALUE;
    public static final String IMAGE_PNG = MediaType.IMAGE_PNG_VALUE;
    public static final String TEXT_PLAIN = MediaType.TEXT_PLAIN_VALUE;
    public static final String MS_WORD_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    public static final List<String> ALLOWED_DOCUMENT_TYPES = List.of(
            APPLICATION_PDF,
            MS_WORD_DOCX,
            TEXT_PLAIN
    );

    public static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            IMAGE_JPEG,
            IMAGE_PNG
    );


}
