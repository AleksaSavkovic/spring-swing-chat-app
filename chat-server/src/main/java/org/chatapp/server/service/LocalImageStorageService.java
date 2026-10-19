package org.chatapp.server.service;

import lombok.extern.slf4j.Slf4j;
import org.chatapp.server.configuration.StorageProperties;
import org.chatapp.server.exceptions.StorageException;
import org.chatapp.server.exceptions.StorageFileNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class LocalImageStorageService implements ImageStorageService{

    private final Path rootLocation;
    private final List<String> imageExtensions = List.of(".jpg", ".jpeg", ".png", ".gif", ".bmp");

    public LocalImageStorageService(StorageProperties properties) {
        if(properties.getLocation().trim().isEmpty()){
            throw new StorageException("File upload location can not be Empty.");
        }

        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @Override
    public Path store(MultipartFile file) {
        String extension = validateAndGetExtension(file);
        String storedFileName = UUID.randomUUID() + extension;

        Path destinationFile = this.rootLocation.resolve(
                Paths.get(storedFileName))
                .normalize().toAbsolutePath();
        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            // For security purposes do not allow storing outside the rootLocation directory
            throw new StorageException("Cannot store file outside current directory.");
        }

        // Copy all data from input stream to a file on given file system path
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to create file in the file system.");
        }
        return destinationFile;
    }

    // Check if the file is empty and obtain the extension
    private String validateAndGetExtension(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }

        // Getting the original filename and extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if (!imageExtensions.contains(extension)) {
            throw new StorageException("Cannot store file with invalid extension, extension: " + extension +
                    ", extensions allowed: " + imageExtensions);
        }
        return extension;
    }

    @Override
    public Stream<Path> loadAll() {
        try(Stream<Path> rootPath = Files.walk(this.rootLocation, 1)) {
            return rootPath
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        }
        catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }
}
