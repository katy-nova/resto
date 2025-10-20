package com.katynova.resto.server_side.exception;

public class ConsistencyException extends RuntimeException {
  public ConsistencyException() {
    super("Ошибка согласованности базы данных");
  }

  public ConsistencyException(String message) {
    super("Ошибка согласованности базы данных" + message);
  }
}
