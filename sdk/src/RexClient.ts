import { Client, type IMessage } from "@stomp/stompjs";
import { FlagCache } from "./FlagCache";
import type {
  ConnectionState,
  EvaluationResult,
  FlagChangedEvent,
  RexClientOptions,
} from "./types";

/**
 * Feature flag client.
 *
 * Bootstraps over REST, then subscribes over WebSocket and serves every subsequent evaluation from
 * a local cache. Flag checks are therefore synchronous and never touch the network.
 *
 * On reconnect the whole cache is refetched rather than resumed. Messages missed while
 * disconnected cannot be replayed by the broker, so anything short of a refetch leaves the client
 * confidently serving stale decisions.
 */
export class RexClient {
  private readonly cache = new FlagCache();
  private readonly options: RexClientOptions;
  private client: Client | undefined;
  private state: ConnectionState = "closed";

  constructor(options: RexClientOptions) {
    this.options = options;
  }

  /** Fetches the initial decisions and opens the socket. */
  async start(): Promise<void> {
    await this.refresh();
    this.connect();
  }

  /** Fetches every decision for this user from the server. */
  async refresh(): Promise<void> {
    const url = new URL(`${this.options.apiUrl}/api/v1/evaluate`);
    url.searchParams.set("userId", this.options.userId);
    url.searchParams.set("environment", this.options.environment);

    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Failed to load flags: ${String(response.status)}`);
    }

    const results = (await response.json()) as EvaluationResult[];
    this.cache.replaceAll(results);
    this.options.onChange?.(this.cache.snapshot());
  }

  /** Synchronous, because it reads the local cache rather than the network. */
  isEnabled(flagName: string, fallback = false): boolean {
    return this.cache.isEnabled(flagName, fallback);
  }

  /** Why the current decision was served, for debugging a surprising result. */
  reasonFor(flagName: string): EvaluationResult["reason"] | undefined {
    return this.cache.reasonFor(flagName);
  }

  snapshot(): readonly EvaluationResult[] {
    return this.cache.snapshot();
  }

  get connectionState(): ConnectionState {
    return this.state;
  }

  async stop(): Promise<void> {
    this.setState("closed");
    await this.client?.deactivate();
    this.client = undefined;
  }

  private connect(): void {
    this.setState("connecting");

    const client = new Client({
      brokerURL: `${this.options.wsUrl}/ws`,
      // stompjs handles the backoff itself; anything missed while down is covered by the
      // refetch on reconnect below.
      reconnectDelay: 2000,
      onConnect: () => {
        this.setState("live");
        client.subscribe(`/topic/flags/${this.options.environment}`, (message: IMessage) => {
          this.handleMessage(message);
        });
        // Refetch on every connect, including reconnects, since missed frames cannot be replayed.
        void this.refresh().catch(() => {
          /* the next reconnect will try again */
        });
      },
      onWebSocketClose: () => {
        if (this.state !== "closed") {
          this.setState("reconnecting");
        }
      },
    });

    this.client = client;
    client.activate();
  }

  private handleMessage(message: IMessage): void {
    let event: FlagChangedEvent;
    try {
      event = JSON.parse(message.body) as FlagChangedEvent;
    } catch {
      return;
    }

    // A flag turning on cannot be applied locally, because inclusion depends on this user's
    // bucket and only the server knows it. Refetch instead of guessing.
    if (this.cache.applyChange(event)) {
      this.options.onChange?.(this.cache.snapshot());
    } else {
      void this.refresh().catch(() => {
        /* the cache keeps serving its last known values */
      });
    }
  }

  private setState(state: ConnectionState): void {
    if (this.state !== state) {
      this.state = state;
      this.options.onConnectionStateChange?.(state);
    }
  }
}
