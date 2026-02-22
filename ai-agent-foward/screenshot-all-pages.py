# -*- coding: utf-8 -*-
"""
Screenshot all pages for visual verification.
Uses Playwright with injected token to bypass auth.
"""
import os
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:5173"
OUTPUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screenshots")

PUBLIC_PAGES = [
    ("/login", "01-login"),
    ("/register", "02-register"),
    ("/forgot-password", "03-forgot-password"),
]

AUTH_PAGES = [
    ("/dashboard", "04-dashboard"),
    ("/agents", "05-agents"),
    ("/knowledge", "06-knowledge"),
    ("/chat", "07-chat"),
    ("/agents/1/workflow", "08-workflow-editor"),
]

def take_screenshot(page, url, name, wait_ms=2000):
    print(f"  [SHOT] {name}: {url}")
    try:
        page.goto(f"{BASE_URL}{url}", wait_until="networkidle", timeout=15000)
    except Exception:
        try:
            page.goto(f"{BASE_URL}{url}", wait_until="load", timeout=10000)
        except Exception as e:
            print(f"     [WARN] Navigation failed: {e}")
            return False
    page.wait_for_timeout(wait_ms)
    path = os.path.join(OUTPUT_DIR, f"{name}.png")
    page.screenshot(path=path, full_page=True)
    print(f"     [OK] Saved: {path}")
    return True

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        print("\n=== Public Pages ===")
        for url, name in PUBLIC_PAGES:
            take_screenshot(page, url, name)

        print("\n=== Injecting auth token ===")
        page.goto(f"{BASE_URL}/login", wait_until="load", timeout=10000)
        page.evaluate("() => sessionStorage.setItem('accessToken', 'fake-token-for-screenshot')")
        print("   Token injected")

        print("\n=== Auth Pages ===")
        for url, name in AUTH_PAGES:
            take_screenshot(page, url, name, wait_ms=3000)

        print("\n=== Workflow Editor (1024px) ===")
        page.set_viewport_size({"width": 1024, "height": 768})
        take_screenshot(page, "/agents/1/workflow", "09-workflow-editor-1024", wait_ms=3000)

        browser.close()

    print(f"\nAll screenshots saved to: {OUTPUT_DIR}")

if __name__ == "__main__":
    main()
