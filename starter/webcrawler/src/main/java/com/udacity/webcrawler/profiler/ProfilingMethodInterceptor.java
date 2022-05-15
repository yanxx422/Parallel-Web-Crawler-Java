package com.udacity.webcrawler.profiler;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate;
  private final ProfilingState state;
  private final ZonedDateTime startTime;

  ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state, ZonedDateTime startTime) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = delegate;
    this.state = state;
    this.startTime = startTime;
  }


  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    Object invoked;
    Instant start = null;
    boolean profiled = method.getAnnotation(Profiled.class) != null;
    if (profiled) {
      start = clock.instant();
    }
    try {
      invoked = method.invoke(delegate, args);
    } catch (InvocationTargetException ex) {
      throw ex.getTargetException();
    } finally {
      if (profiled) {
        Duration duration = Duration.between(start, clock.instant());
        state.record(delegate.getClass(), method, duration);
      }
    }

    return invoked;
  }
}