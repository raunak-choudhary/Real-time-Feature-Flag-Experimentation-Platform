package com.rex.evaluation;

/**
 * Decides whether a user enters an experiment and, if so, which variant they see.
 *
 * <p>Two independent draws, deliberately. The first decides entry against the traffic allocation,
 * the second splits entrants between control and test. Deriving both from a single hash would
 * correlate them, so raising traffic from 20 to 40 percent would not only admit new users but
 * reshuffle which variant the existing ones were in.
 *
 * <p>This decides only. Persistence and stickiness belong to the caller: an assignment already
 * recorded always wins, because recomputing one mid experiment would invalidate the result.
 */
public final class VariantAssigner {

  private static final String ENTRY_NAMESPACE_SUFFIX = ":entry";
  private static final String SPLIT_NAMESPACE_SUFFIX = ":split";

  private VariantAssigner() {}

  /** Whether the user falls inside the experiment's traffic allocation. */
  public static boolean isEnrolled(String experimentKey, String userId, int trafficPercentage) {
    return BucketHasher.isInRollout(
        experimentKey + ENTRY_NAMESPACE_SUFFIX, userId, trafficPercentage);
  }

  /**
   * Splits an enrolled user between control and test.
   *
   * <p>An even split is the only sensible default: an uneven one needs a reason, and a silent
   * uneven split is how experiments quietly lose their power.
   */
  public static Variant assignVariant(String experimentKey, String userId) {
    int bucket = BucketHasher.bucketFor(experimentKey + SPLIT_NAMESPACE_SUFFIX, userId);
    return bucket < BucketHasher.BUCKET_COUNT / 2 ? Variant.CONTROL : Variant.TEST;
  }

  /** The assignment bucket, stored so an assignment can be explained after the fact. */
  public static int assignmentBucket(String experimentKey, String userId) {
    return BucketHasher.bucketFor(experimentKey + SPLIT_NAMESPACE_SUFFIX, userId);
  }

  /** Which side of an experiment a user is on. */
  public enum Variant {
    CONTROL,
    TEST
  }
}
