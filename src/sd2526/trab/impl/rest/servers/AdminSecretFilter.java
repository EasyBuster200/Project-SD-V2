package sd2526.trab.impl.rest.servers;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import sd2526.trab.impl.utils.ServerSecret;

@Provider
public class AdminSecretFilter implements ContainerRequestFilter {

    private static final String ADMIN_PATH_SEGMENT = "/admin";

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String path = ctx.getUriInfo().getPath();
        if (!path.contains(ADMIN_PATH_SEGMENT))
            return;

        String expected = ServerSecret.get();
        String provided = ctx.getHeaderString(ServerSecret.HEADER);

        if (expected == null || expected.isEmpty()) {
            return;
        }

        if (!expected.equals(provided)) {
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }
}