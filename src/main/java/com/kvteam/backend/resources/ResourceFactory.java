package com.kvteam.backend.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.kvteam.backend.exceptions.ResourceException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Solovyev on 13/04/2017.
 */
@SuppressWarnings("OverlyBroadCatchBlock")
@Component
public class ResourceFactory {
    private final ObjectMapper objectMapper;

    public ResourceFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T extends Resource> List<T> getFromDir(String path, Class<T> clazz){
        final URL resourceDescriptor;
        try {
            resourceDescriptor = Resources.getResource(path);
        } catch (IllegalArgumentException ex) {
            throw new ResourceException("Unable to find resource " + path, ex);
        }
        final List<T> resources = new ArrayList<>();
        final File rootDir = new File(resourceDescriptor.getFile());
        for (File f : Files.fileTreeTraverser().preOrderTraversal(rootDir)) {
            if(!f.isDirectory()) {
                resources.add(get(f, clazz));
            }
        }
        return resources;
    }


    public RawResource getRaw(String path) {
        final URL resourceDescriptor;
        try {
            resourceDescriptor = Resources.getResource(path);
        } catch (IllegalArgumentException ex) {
            throw new ResourceException("Unable to find resource " + path, ex);
        }

        return getRaw(new File(resourceDescriptor.getFile()));
    }

    public RawResource getRaw(File file){
        final RawResource rawResource;
        try {
            final String resourceContent = Files.toString(file, Charset.forName("UTF-8"));

            rawResource = objectMapper.readValue(resourceContent, RawResource.class);
        } catch (IOException e) {
            throw new ResourceException("Unable to parse resource " + file.getPath(), e);
        }
        return rawResource;
    }


    public <T extends Resource> T get(String path, Class<T> clazz) {
        final URL resourceDescriptor;
        try {
            resourceDescriptor = Resources.getResource(path);
        } catch (IllegalArgumentException ex) {
            throw new ResourceException("Unable to find resource " + path, ex);
        }
        return get(new File(resourceDescriptor.getFile()), clazz);
    }

    public <T extends Resource> T get(File file, Class<T> clazz) {
        final T resource;
        try {
            resource = objectMapper.readValue(file, clazz);
        } catch (IOException e) {
            throw new ResourceException(
                    "Failed constructing resource object "
                            + file.getPath()
                            + " of type "
                            + clazz.getName(), e);
        }

        if(!resource.getType().equals(clazz.getName())) {
            throw new ResourceException(
                    "type mismatch for resource "
                            + file.getPath()
                            + ". Expected "
                            + clazz.getName()
                            + " , but got "
                            + resource.getType());
        }

        return resource;
    }
}
