package org.carrot2.dcs;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.carrot2.dcs.Loggers.CONSOLE;

public class JettyContainer {
  private final int port;
  private final Path webappContexts;
  private Server server;
  private ServerConnector connector;

  public JettyContainer(int port, Path contexts) {
    this.port = port;
    this.webappContexts = contexts;
  }

  public void start() throws Exception {
    server = createServer();
    addContexts(server, webappContexts);
    server.start();
  }

  public void join() throws InterruptedException {
    server.join();
  }

  public void stop() throws Exception {
    server.stop();
  }

  public int getPort() {
    return connector.getLocalPort();
  }

  private void addContexts(Server server, Path contexts) throws IOException {
    List<Path> webapps;
    try (Stream<Path> list = Files.list(contexts)) {
      webapps = list
          .filter(dir -> !Files.isDirectory(dir.resolve("WEB-INF").resolve("web.xml")))
          .collect(Collectors.toList());
    }

    ArrayList<ContextHandler> handlers = new ArrayList<>();
    for (Path context : webapps) {
      if (!Files.isRegularFile(context.resolve("WEB-INF").resolve("web.xml"))) {
        throw new RuntimeException("Not a web application context folder?: "
          + context.toAbsolutePath());
      }

      String ctxName = context.getFileName().toString();
      String ctxPath = "root".equalsIgnoreCase(ctxName) ? "/" : "/" + ctxName;

      WebAppContext ctx = new WebAppContext();
      ctx.setContextPath(ctxPath);
      ctx.setThrowUnavailableOnStartupException(true);
      ctx.setWar(context.normalize().toAbsolutePath().toString());
      CONSOLE.info("Deploying context '{}' at: {}.", ctxName, ctxPath);
      handlers.add(ctx);
    }

    server.setHandler(new ContextHandlerCollection(handlers.toArray(new ContextHandler[0])));
  }

  private LifeCycle.Listener createLifecycleLogger(Server server, ServerConnector connector) {
    return new AbstractLifeCycle.AbstractLifeCycleListener() {
      @Override
      public void lifeCycleStarted(LifeCycle event) {
        CONSOLE.info("Service started on port {}.", connector.getLocalPort());
      }

      @Override
      public void lifeCycleStopping(LifeCycle event) {
        CONSOLE.info("Service stopping...");
      }

      @Override
      public void lifeCycleStopped(LifeCycle event) {
        CONSOLE.info("Service stopped.");
      }

      @Override
      public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        if (cause instanceof BindException) {
          CONSOLE.error("Network port binding error: " + cause.getMessage());
        } else {
          CONSOLE.error("Server failed to start.", cause);
        }

        try {
          server.stop();
        } catch (Exception e) {
          CONSOLE.error("Could not stop the server.", e);
        }
      }
    };
  }

  private Server createServer() {
    QueuedThreadPool threadPool = new QueuedThreadPool() {
      private AtomicInteger tid = new AtomicInteger();

      @Override
      protected Thread newThread(Runnable runnable) {
        return new Thread(() -> {
          Thread.currentThread().setName("T" + tid.incrementAndGet());
          runnable.run();
        });
      }
    };
    threadPool.setMaxThreads(50);

    Server server = new Server(threadPool);

    connector = new ServerConnector(server);
    connector.setPort(port);
    server.addConnector(connector);
    server.addLifeCycleListener(createLifecycleLogger(server, connector));
    return server;
  }
}