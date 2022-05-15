package com.udacity.webcrawler.profiler;

import com.udacity.webcrawler.json.CrawlerConfiguration;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }


  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    Method[] methods = klass.getMethods();
    boolean isProfiled = false;
    for (Method method : methods) {
      isProfiled = method.getAnnotation(Profiled.class) != null;
      if (isProfiled) {
        break;
      }
    }

    if (!isProfiled) {
      throw new IllegalArgumentException(klass.getName() + "doesn't have profiled methods.");
    }
    ProfilingMethodInterceptor interceptor = new ProfilingMethodInterceptor(clock, delegate, state, startTime);
    Object proxy = Proxy.newProxyInstance(
            ProfilerImpl.class.getClassLoader(),
            new Class[]{klass},
            interceptor
    );
    return (T)proxy;
  }
  @Override
  public void writeData(Path path) {
    Objects.requireNonNull(path);

    try (Writer writer = Files.newBufferedWriter(path)) {
      writeData(writer);
      writer.flush();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }


  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
