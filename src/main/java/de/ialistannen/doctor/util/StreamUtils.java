package de.ialistannen.doctor.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamUtils {

  public static <T> Collector<T, ?, Map<Integer, List<T>>> partition(int groupSize) {
    var counter = new Object() {
      int i = 0;
    };
    return Collectors.groupingBy(
        it -> (counter.i++) / groupSize,
        Collectors.toList()
    );
  }

  public static <T> Map<Integer, T> enumerated(Stream<T> stream) {
    int counter = 0;
    Map<Integer, T> result = new HashMap<>();

    for (T t : (Iterable<? extends T>) stream::iterator) {
      result.put(counter++, t);
    }

    return result;
  }
}
