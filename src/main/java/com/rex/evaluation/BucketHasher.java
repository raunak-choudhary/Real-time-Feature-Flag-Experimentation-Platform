package com.rex.evaluation;

import java.nio.charset.StandardCharsets;

/**
 * Maps a user onto a stable bucket for percentage rollouts and variant splits.
 *
 * <p>Uses MurmurHash3, not {@link String#hashCode()}. Two problems ruled out the JDK hash. It has
 * poor avalanche behaviour, so user ids that differ by one character land in adjacent buckets and a
 * rollout drawn from sequential ids is badly skewed. And {@code Math.abs(Integer.MIN_VALUE)}
 * returns {@code Integer.MIN_VALUE}, which is negative, so one unlucky id produces a negative
 * bucket index.
 *
 * <p>Buckets are basis points rather than whole percentages, so a rollout can move in hundredths of
 * a percent. That matters for the first stage of a progressive rollout, where one percent of a
 * large user base is still a lot of people.
 *
 * <p>This class is pure by design: no Spring, no repository, no clock. That is what makes the
 * distribution properties testable.
 */
public final class BucketHasher {

  /** Buckets span 0 to 9999, so a percentage maps to a hundred buckets. */
  public static final int BUCKET_COUNT = 10_000;

  private static final int SEED = 0x9747b28c;
  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;

  private BucketHasher() {}

  /**
   * Returns the bucket for a user within a namespace, in the range 0 to 9999.
   *
   * <p>The namespace is normally the flag or experiment key. Including it means a user's bucket in
   * one experiment says nothing about their bucket in another, so running several experiments at
   * once does not correlate their populations.
   */
  public static int bucketFor(String namespace, String userId) {
    String key = namespace + ":" + userId;
    int hash = murmur3(key.getBytes(StandardCharsets.UTF_8), SEED);
    // Unsigned conversion, so the Math.abs(Integer.MIN_VALUE) trap cannot recur.
    return (int) (Integer.toUnsignedLong(hash) % BUCKET_COUNT);
  }

  /** True when the user falls inside the given percentage for this namespace. */
  public static boolean isInRollout(String namespace, String userId, int percentage) {
    if (percentage <= 0) {
      return false;
    }
    if (percentage >= 100) {
      return true;
    }
    return bucketFor(namespace, userId) < percentage * 100;
  }

  /** The bucket expressed as a whole percentage, for display and for legacy callers. */
  public static int percentileFor(String namespace, String userId) {
    return bucketFor(namespace, userId) / 100;
  }

  private static int murmur3(byte[] data, int seed) {
    int hash = seed;
    int length = data.length;
    int blocks = length >> 2;

    for (int i = 0; i < blocks; i++) {
      int index = i << 2;
      int k =
          (data[index] & 0xff)
              | ((data[index + 1] & 0xff) << 8)
              | ((data[index + 2] & 0xff) << 16)
              | ((data[index + 3] & 0xff) << 24);
      hash ^= mixKey(k);
      hash = Integer.rotateLeft(hash, 13) * 5 + 0xe6546b64;
    }

    int tail = blocks << 2;
    int remainder = 0;
    switch (length & 3) {
      case 3:
        remainder ^= (data[tail + 2] & 0xff) << 16;
        remainder ^= (data[tail + 1] & 0xff) << 8;
        remainder ^= data[tail] & 0xff;
        break;
      case 2:
        remainder ^= (data[tail + 1] & 0xff) << 8;
        remainder ^= data[tail] & 0xff;
        break;
      case 1:
        remainder ^= data[tail] & 0xff;
        break;
      default:
        break;
    }
    hash ^= mixKey(remainder);

    hash ^= length;
    return finalMix(hash);
  }

  private static int mixKey(int k) {
    int key = k * C1;
    key = Integer.rotateLeft(key, 15);
    return key * C2;
  }

  /** The avalanche step. Without it neighbouring inputs would still produce neighbouring hashes. */
  private static int finalMix(int hash) {
    int h = hash;
    h ^= h >>> 16;
    h *= 0x85ebca6b;
    h ^= h >>> 13;
    h *= 0xc2b2ae35;
    h ^= h >>> 16;
    return h;
  }
}
