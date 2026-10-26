package com.oussema.cloud.validator;

import com.oussema.cloud.exception.StorageException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Implements the validation logic for the {@link ValidFile} annotation.
 * This validator checks if a given file adheres to the allowed types as defined
 * in the annotation and validates the file's content using the FileValidator component.
 */
@Component
public class FileValidatorConstraint implements ConstraintValidator<ValidFile, MultipartFile> {

    private List<String> allowedTypes;
    private final FileValidator fileValidator;

    public FileValidatorConstraint(final FileValidator fileValidator) {
        this.fileValidator = fileValidator;
    }

    @Override
    public void initialize(ValidFile constraintAnnotation) {
        this.allowedTypes = Arrays.asList(constraintAnnotation.allowedTypes());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true;
        }

        try {
            fileValidator.validate(file, this.allowedTypes);

            // If the validate() method doesn't throw an exception, the file is valid.
            return true;

        } catch (StorageException | IOException e) {

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(e.getMessage())
                    .addConstraintViolation();

            return false;
        }
    }
}