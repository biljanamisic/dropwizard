package io.dropwizard.jetty;

import java.io.IOException;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Request;
import org.junit.Test;
import org.mockito.InOrder;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NonblockingServletHolderTest {
    private final Servlet servlet = mock(Servlet.class);
    private final NonblockingServletHolder holder = new NonblockingServletHolder(servlet);
    private final Request baseRequest = mock(Request.class);
    private final ServletRequest request = mock(ServletRequest.class);
    private final ServletResponse response = mock(ServletResponse.class);

    @Test
    public void hasAServlet() throws Exception {
        assertThat(holder.getServlet())
                .isEqualTo(servlet);
    }

    @Test
    public void servicesRequests() throws Exception {
        holder.handle(baseRequest, request, response);

        verify(servlet).service(request, response);
    }

    @Test
    public void servicesRequestHandleEofException() throws Exception {
        doThrow(new EofException()).when(servlet).service(eq(request), eq(response));
        assertThatCode(() -> {
            holder.handle(baseRequest, request, response);
        }).doesNotThrowAnyException();
        verify(servlet).service(request, response);
    }

    @Test
    public void servicesRequestException() throws Exception {
        doThrow(new IOException()).when(servlet).service(eq(request), eq(response));
        assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
            holder.handle(baseRequest, request, response);
        });
    }

    @Test
    public void temporarilyDisablesAsyncRequestsIfDisabled() throws Exception {
        holder.setAsyncSupported(false);

        holder.handle(baseRequest, request, response);

        final InOrder inOrder = inOrder(baseRequest, servlet);

        inOrder.verify(baseRequest).setAsyncSupported(false, null);
        inOrder.verify(servlet).service(request, response);
    }

    @Test
    public void isEagerlyInitialized() throws Exception {
        assertThat(holder.getInitOrder())
                .isEqualTo(1);
    }
}
