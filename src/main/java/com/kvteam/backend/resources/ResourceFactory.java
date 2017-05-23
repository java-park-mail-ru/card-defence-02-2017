package com.kvteam.backend.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.kvteam.backend.exceptions.ResourceException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@SuppressWarnings({"OverlyBroadCatchBlock", "SameParameterValue"})
@Component
public class ResourceFactory {
    private final ObjectMapper objectMapper;

    public ResourceFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RawResource getRaw(String path) {
        final URL resourceDescriptor;
        try {
            resourceDescriptor = Resources.getResource(path);
        } catch (IllegalArgumentException ex) {
            throw new ResourceException("Unable to find resource " + path, ex);
        }

        return getRaw(resourceDescriptor);
    }

    public RawResource getRaw(URL resourceDescriptor){
        final RawResource rawResource;
        try {
            rawResource = objectMapper.readValue(resourceDescriptor, RawResource.class);
        } catch (IOException e) {
            throw new ResourceException("Unable to parse resource " + resourceDescriptor.getPath(), e);
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
        return get(resourceDescriptor, clazz);
    }

    public <T extends Resource> T get(URL resourceDescriptor, Class<T> clazz) {
        final T resource;
        try {
            resource = objectMapper.readValue(resourceDescriptor, clazz);
        } catch (IOException e) {
            throw new ResourceException(
                    "Failed constructing resource object "
                            + resourceDescriptor.getPath()
                            + " of type "
                            + clazz.getName(), e);
        }

        if(!resource.getType().equals(clazz.getName())) {
            throw new ResourceException(
                    "type mismatch for resource "
                            + resourceDescriptor.getPath()
                            + ". Expected "
                            + clazz.getName()
                            + " , but got "
                            + resource.getType());
        }

        return resource;
    }


    public String[] getJsonStringArray(String path){
        final URL resourceDescriptor;
        try {
            resourceDescriptor = Resources.getResource(path);
        } catch (IllegalArgumentException ex) {
            throw new ResourceException("Unable to find resource " + path, ex);
        }
        final String[] strings;
        try {
            strings = objectMapper.readValue(resourceDescriptor, String[].class);
        } catch (IOException e) {
            throw new ResourceException(
                    "Failed constructing resource object "
                            + resourceDescriptor.getPath()
                            + " of type "
                            + String[].class.getName(), e);
        }
        return strings;
    }
}
