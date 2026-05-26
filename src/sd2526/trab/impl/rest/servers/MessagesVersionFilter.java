package sd2526.trab.impl.rest.servers;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import sd2526.trab.impl.utils.SyncPoint;

@Provider
public class MessagesVersionFilter implements ContainerRequestFilter, ContainerResponseFilter {

  public static final String VERSION_HEADER = "X-MESSAGES-VERSION";

  public static final ThreadLocal<Long> requestVersion = new ThreadLocal<>();

  /** Incoming: Parses the header (if any) and stashes it in the ThreadLocal */
  @Override
  public void filter(ContainerRequestContext req) throws IOException {
    String v = req.getHeaderString(VERSION_HEADER);
    if (v != null && !v.isEmpty()) {
      try {
        requestVersion.set(Long.parseLong(v));
      } catch (NumberFormatException x) {
        requestVersion.set(0L);
      }
    } else {
      requestVersion.set(0L);
    }
  }

  /** Outgoing: stamp outgoing responses with the current applied version. */
  @Override
  public void filter(ContainerRequestContext req, ContainerResponseContext resp) throws IOException {
    long current = SyncPoint.getSyncPoint().currentVersion();
    resp.getHeaders().putSingle(VERSION_HEADER, Long.toString(current));
    requestVersion.remove();
  }

  /** Helper method to read the current request's version */
  public static long currentRequestVersion() {
    Long v = requestVersion.get();
    return v == null ? 0L : v;
  }
}