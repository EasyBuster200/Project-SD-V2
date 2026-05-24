package sd2526.trab.impl.replicated;

import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.impl.utils.JSON;

public record ResultEnvelope(String errorCode, String valueJson) {

  public static <T> String encode(Result<T> r) {
    if (r.isOK()) {
      String json = r.value() == null ? null : JSON.encode(r.value());
      return JSON.encode(new ResultEnvelope("OK", json));
    } else {
      return JSON.encode(new ResultEnvelope(r.error().name(), null));
    }
  }

  public static <T> Result<T> decode(String s, Class<T> valueType) {
    if (s == null) {
      return Result.ok();
    }
    ResultEnvelope env = JSON.decode(s, ResultEnvelope.class);
    if ("OK".equals(env.errorCode)) {
      if (env.valueJson == null || valueType == Void.class)
        return Result.ok();
      T value = JSON.decode(env.valueJson, valueType);
      return Result.ok(value);
    } else {
      return Result.error(ErrorCode.valueOf(env.errorCode));
    }
  }
}