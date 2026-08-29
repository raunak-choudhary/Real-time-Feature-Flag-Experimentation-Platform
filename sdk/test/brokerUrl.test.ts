import { describe, expect, it } from "vitest";
import { brokerUrlFrom } from "../src/RexClient";

describe("brokerUrlFrom", () => {
  it("appends the endpoint path to a bare origin", () => {
    expect(brokerUrlFrom("wss://api.example.com")).toBe("wss://api.example.com/ws");
  });

  it("does not double the path when the origin already carries it", () => {
    expect(brokerUrlFrom("wss://api.example.com/ws")).toBe("wss://api.example.com/ws");
  });

  it("tolerates a trailing slash", () => {
    expect(brokerUrlFrom("wss://api.example.com/")).toBe("wss://api.example.com/ws");
    expect(brokerUrlFrom("wss://api.example.com/ws/")).toBe("wss://api.example.com/ws");
  });

  it("leaves a host whose name merely ends in ws alone", () => {
    expect(brokerUrlFrom("wss://myws.example.com")).toBe("wss://myws.example.com/ws");
  });

  it("works for an unencrypted local origin", () => {
    expect(brokerUrlFrom("ws://localhost:8080")).toBe("ws://localhost:8080/ws");
  });
});
