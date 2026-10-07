package repro;

public final class Check {
  public static void main(String[] args) throws Exception {
    ProbeOuterClass.Probe child = ProbeOuterClass.Probe.newBuilder().setValue("child").build();
    ProbeOuterClass.Probe message = ProbeOuterClass.Probe.newBuilder()
        .putChildren("key", child).build();
    ProbeOuterClass.Probe decoded = ProbeOuterClass.Probe.parseFrom(message.toByteArray());
    if (!decoded.getChildrenOrThrow("key").equals(child)) {
      throw new AssertionError("Message-valued map did not round-trip");
    }
  }
}
