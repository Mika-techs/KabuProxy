package de.mik.kabuproxy.web;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.faces.application.Resource;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.application.ResourceHandlerWrapper;
import jakarta.faces.application.ResourceWrapper;

/**
 * Appends the build stamp ({@code &v=...}) to the URLs of our own resources (library {@code kabu}), so a new build
 * busts the browser cache of kabu.css/kabu.js. Registered in faces-config.xml.
 */
public class VersionedResourceHandler extends ResourceHandlerWrapper
{
    private static final String LIBRARY = "kabu";

    private volatile String stamp;

    public VersionedResourceHandler(ResourceHandler wrapped)
    {
        super(wrapped);
    }

    @Override
    public Resource createResource(String resourceName, String libraryName)
    {
        return versioned(super.createResource(resourceName, libraryName), libraryName);
    }

    @Override
    public Resource createResource(String resourceName, String libraryName, String contentType)
    {
        return versioned(super.createResource(resourceName, libraryName, contentType), libraryName);
    }

    private Resource versioned(Resource resource, String libraryName)
    {
        if (resource == null || !LIBRARY.equals(libraryName))
        {
            return resource;
        }
        String version = stamp();
        return new ResourceWrapper(resource)
        {
            @Override
            public String getRequestPath()
            {
                String path = super.getRequestPath();
                return path + (path.contains("?") ? "&" : "?") + "v=" + version;
            }
        };
    }

    private String stamp()
    {
        // the handler is created before CDI is usable, so look the stamp up on first use
        if (stamp == null)
        {
            stamp = CDI.current().select(BuildInfo.class).get().stamp();
        }
        return stamp;
    }
}
