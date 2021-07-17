package de.ialistannen.docfork.util;

import java.util.Optional;

public class Result<T, E extends Throwable> {

  private final T value;
  private final E error;

  private Result(T value, E error) {
    this.value = value;
    this.error = error;
  }

  public Optional<T> getValue() {
    return Optional.ofNullable(value);
  }

  public Optional<E> getError() {
    return Optional.ofNullable(error);
  }

  public T getOrThrow() throws E {
    if (value != null) {
      return value;
    }
    throw error;
  }

  public static <T, E extends Throwable> Result<T, E> ok(T val) {
    return new Result<>(val, null);
  }

  public static <T, E extends Throwable> Result<T, E> error(E error) {
    return new Result<>(null, error);
  }
}
