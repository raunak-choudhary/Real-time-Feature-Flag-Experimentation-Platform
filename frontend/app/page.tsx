import { AuditFeed } from "../components/AuditFeed";
import { LiveFlagPanel } from "../components/LiveFlagPanel";
import { ThemeToggle } from "../components/ThemeToggle";
import { config } from "../lib/config";
import { loadAuditFeed, loadFlags } from "../lib/serverFetch";

/**
 * The console.
 *
 * A server component, so the first paint carries real flags and a real audit feed rather than an
 * empty shell that fills in afterwards. The live panel is a client component that takes over from
 * there.
 */
export default async function DashboardPage() {
  const [flags, audit] = await Promise.all([loadFlags(), loadAuditFeed()]);

  return (
    <main className="shell">
      <header className="masthead">
        <div>
          <h1>REX Platform</h1>
          <p className="subtitle">
            Feature flags and experiments · {config.environment}
          </p>
        </div>
        <ThemeToggle />
      </header>

      <div className="grid">
        <LiveFlagPanel initialFlags={flags} />

        <section className="panel">
          <h2>Recent changes</h2>
          <AuditFeed entries={audit} />
        </section>
      </div>
    </main>
  );
}
