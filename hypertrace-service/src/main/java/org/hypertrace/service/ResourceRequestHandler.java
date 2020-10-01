package org.hypertrace.service;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceRequestHandler extends HandlerWrapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRequestHandler.class);

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
  {
    LOGGER.info("handling request:{}", target);

    if (!target.startsWith("/graphql")) {
      Handler handler = _handler;
      if (handler != null) {
        LOGGER.info("handling request:{}, baseRequest:{}, servletre:{}, ishandled:{}, requestmethod:{}",
            target, baseRequest, request, baseRequest.isHandled(), request.getMethod());
        handler.handle(target, baseRequest, request, response);
      }
    }
  }
}
