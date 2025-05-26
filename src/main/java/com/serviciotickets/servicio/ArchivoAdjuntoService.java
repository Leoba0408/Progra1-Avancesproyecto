package com.serviciotickets.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

@Service
public class ArchivoAdjuntoService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png"};
    private final String[] ALLOWED_ATTACHMENT_TYPES = {
        "image/jpeg", "image/png", "application/pdf", 
        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    };

    public String guardarLogo(MultipartFile file) throws IOException {
        validarArchivoImagen(file);
        return guardarArchivo(file, "logos");
    }

    public String guardarAdjuntoTicket(MultipartFile file) throws IOException {
        validarArchivoAdjunto(file);
        return guardarArchivo(file, "tickets");
    }

    private String guardarArchivo(MultipartFile file, String subDir) throws IOException {
        Path uploadPath = Paths.get(uploadDir, subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return subDir + "/" + fileName;
    }

    private void validarArchivoImagen(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 2MB");
        }

        String contentType = file.getContentType();
        if (!Arrays.asList(ALLOWED_IMAGE_TYPES).contains(contentType)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo se permiten archivos JPG y PNG");
        }
    }

    private void validarArchivoAdjunto(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 2MB");
        }

        String contentType = file.getContentType();
        if (!Arrays.asList(ALLOWED_ATTACHMENT_TYPES).contains(contentType)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido");
        }
    }

    public void eliminarArchivo(String filePath) throws IOException {
        Path path = Paths.get(uploadDir, filePath);
        Files.deleteIfExists(path);
    }
} 