package com.bikestore.api.service;

import com.bikestore.api.entity.Product;
import com.bikestore.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.product-images.s3-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class ProductImageCleanupService {

    private final ProductRepository productRepository;
    private final S3Client s3Client;

    @org.springframework.beans.factory.annotation.Value("${aws.s3.bucket.name:}")
    private String bucketName;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupInvalidProductImages() {
        if (bucketName == null || bucketName.isBlank()) {
            log.warn("Skipping product image cleanup because aws.s3.bucket.name is empty.");
            return;
        }

        List<Product> products = productRepository.findAll();
        int updatedProducts = 0;
        int removedImages = 0;

        for (Product product : products) {
            List<String> currentImages = product.getImages();
            if (currentImages == null || currentImages.isEmpty()) {
                continue;
            }

            VerificationResult verificationResult = filterExistingS3Images(currentImages);
            if (verificationResult.hasUnknownError()) {
                log.warn("Skipping image cleanup for product {} due to S3 verification errors.", product.getId());
                continue;
            }

            List<String> validImages = verificationResult.validImages();
            if (validImages.size() != currentImages.size()) {
                removedImages += currentImages.size() - validImages.size();
                currentImages.clear();
                currentImages.addAll(validImages);
                updatedProducts++;
            }
        }

        if (updatedProducts > 0) {
            productRepository.flush();
        }

        log.info("Product image cleanup completed. Updated products: {}, removed images: {}", updatedProducts, removedImages);
    }

    private VerificationResult filterExistingS3Images(List<String> images) {
        List<String> validImages = new ArrayList<>();
        boolean hasUnknownError = false;

        for (String imageUrl : images) {
            ImageVerificationStatus status = verifyImageExistsInS3(imageUrl);
            if (status == ImageVerificationStatus.VALID) {
                validImages.add(imageUrl);
            } else if (status == ImageVerificationStatus.UNKNOWN_ERROR) {
                hasUnknownError = true;
                break;
            }
        }

        return new VerificationResult(validImages, hasUnknownError);
    }

    private ImageVerificationStatus verifyImageExistsInS3(String imageUrl) {
        String objectKey = extractObjectKey(imageUrl);
        if (objectKey == null) {
            return ImageVerificationStatus.MISSING;
        }

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            return ImageVerificationStatus.VALID;
        } catch (NoSuchKeyException e) {
            return ImageVerificationStatus.MISSING;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return ImageVerificationStatus.MISSING;
            }
            log.warn("Failed to verify S3 image {} due to S3 error: {}", imageUrl, e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            return ImageVerificationStatus.UNKNOWN_ERROR;
        } catch (Exception e) {
            log.warn("Failed to verify S3 image {} due to unexpected error: {}", imageUrl, e.getMessage());
            return ImageVerificationStatus.UNKNOWN_ERROR;
        }
    }

    private String extractObjectKey(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(imageUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return null;
            }
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private enum ImageVerificationStatus {
        VALID,
        MISSING,
        UNKNOWN_ERROR
    }

    private record VerificationResult(List<String> validImages, boolean hasUnknownError) {
    }
}
