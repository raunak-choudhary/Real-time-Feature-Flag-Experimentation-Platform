package com.rex.evaluation;

import java.util.List;

/** How a targeting rule compares a user attribute against its configured values. */
public enum RuleOperator {
  EQUALS {
    @Override
    boolean matches(String actual, List<String> expected) {
      return expected.size() == 1 && expected.get(0).equals(actual);
    }
  },
  NOT_EQUALS {
    @Override
    boolean matches(String actual, List<String> expected) {
      return !EQUALS.matches(actual, expected);
    }
  },
  IN {
    @Override
    boolean matches(String actual, List<String> expected) {
      return expected.contains(actual);
    }
  },
  NOT_IN {
    @Override
    boolean matches(String actual, List<String> expected) {
      return !expected.contains(actual);
    }
  },
  CONTAINS {
    @Override
    boolean matches(String actual, List<String> expected) {
      return expected.stream().anyMatch(actual::contains);
    }
  },
  GREATER_THAN {
    @Override
    boolean matches(String actual, List<String> expected) {
      return compareNumeric(actual, expected) > 0;
    }
  },
  LESS_THAN {
    @Override
    boolean matches(String actual, List<String> expected) {
      return compareNumeric(actual, expected) < 0;
    }
  },
  /**
   * Semantic version comparison.
   *
   * <p>Separate from GREATER_THAN because version strings compared lexically put 1.10.0 below
   * 1.9.0, which is how "roll out to version 1.10 and above" silently misses the users it was meant
   * to reach.
   */
  VERSION_GREATER_OR_EQUAL {
    @Override
    boolean matches(String actual, List<String> expected) {
      return expected.size() == 1 && compareVersions(actual, expected.get(0)) >= 0;
    }
  };

  abstract boolean matches(String actual, List<String> expected);

  private static int compareNumeric(String actual, List<String> expected) {
    if (expected.size() != 1) {
      return 0;
    }
    try {
      return Double.compare(Double.parseDouble(actual), Double.parseDouble(expected.get(0)));
    } catch (NumberFormatException exception) {
      // A non numeric attribute simply does not match, rather than failing the whole evaluation.
      return 0;
    }
  }

  static int compareVersions(String left, String right) {
    String[] leftParts = left.split("\\.");
    String[] rightParts = right.split("\\.");
    int length = Math.max(leftParts.length, rightParts.length);

    for (int i = 0; i < length; i++) {
      int leftPart = segment(leftParts, i);
      int rightPart = segment(rightParts, i);
      if (leftPart != rightPart) {
        return Integer.compare(leftPart, rightPart);
      }
    }
    return 0;
  }

  private static int segment(String[] parts, int index) {
    if (index >= parts.length) {
      return 0;
    }
    try {
      return Integer.parseInt(parts[index].replaceAll("[^0-9].*$", ""));
    } catch (NumberFormatException exception) {
      return 0;
    }
  }
}
