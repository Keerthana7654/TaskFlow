import { useState } from 'react';
import Sidebar from './Sidebar';

export default function AppShell({ children }) {
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  return (
    <div className="app-shell">
      {/* Mobile-only top bar with hamburger toggle */}
      <header className="mobile-topbar">
        <button
          className="hamburger-btn"
          onClick={() => setMobileNavOpen(true)}
          aria-label="Open menu"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
        <div className="mobile-topbar-logo">Task<span>Flow</span></div>
      </header>

      {/* Dimmed backdrop behind the slide-in sidebar on mobile */}
      {mobileNavOpen && (
        <div
          className="sidebar-backdrop"
          onClick={() => setMobileNavOpen(false)}
        />
      )}

      <Sidebar
        mobileNavOpen={mobileNavOpen}
        onClose={() => setMobileNavOpen(false)}
      />

      <main className="main-content">{children}</main>
    </div>
  );
}
