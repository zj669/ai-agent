from playwright.sync_api import sync_playwright
import json

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 900})
    page.goto("http://localhost:5173/login")
    page.wait_for_load_state("networkidle")

    # Take screenshot
    page.screenshot(path="login-visual-check.png", full_page=True)
    print("Screenshot saved: login-visual-check.png")

    # Detailed visual analysis
    analysis = page.evaluate("""() => {
        const result = {};

        // 1. Check all visible text content and positions
        const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
        const texts = [];
        while (walker.nextNode()) {
            const node = walker.currentNode;
            const range = document.createRange();
            range.selectNodeContents(node);
            const rect = range.getBoundingClientRect();
            const text = node.textContent.trim();
            if (text && rect.width > 0 && rect.height > 0) {
                const cs = getComputedStyle(node.parentElement);
                texts.push({
                    text: text.substring(0, 50),
                    x: Math.round(rect.x),
                    y: Math.round(rect.y),
                    w: Math.round(rect.width),
                    h: Math.round(rect.height),
                    color: cs.color,
                    fontSize: cs.fontSize,
                    fontWeight: cs.fontWeight
                });
            }
        }
        result.visibleTexts = texts;

        // 2. Check backgrounds of major sections
        const sections = [
            { sel: 'body', name: 'body' },
            { sel: 'main', name: 'main' },
            { sel: 'main > div', name: 'card' },
            { sel: 'aside', name: 'aside' },
            { sel: 'section', name: 'formSection' },
            { sel: 'button[type=submit]', name: 'submitBtn' },
        ];
        result.sectionStyles = {};
        for (const s of sections) {
            const el = document.querySelector(s.sel);
            if (el) {
                const cs = getComputedStyle(el);
                const rect = el.getBoundingClientRect();
                result.sectionStyles[s.name] = {
                    bg: cs.backgroundColor,
                    color: cs.color,
                    border: cs.border,
                    shadow: cs.boxShadow !== 'none' ? 'yes' : 'no',
                    borderRadius: cs.borderRadius,
                    rect: `${Math.round(rect.x)},${Math.round(rect.y)} ${Math.round(rect.width)}x${Math.round(rect.height)}`
                };
            }
        }

        // 3. Check inputs
        const inputs = document.querySelectorAll('input');
        result.inputs = Array.from(inputs).map(inp => {
            const cs = getComputedStyle(inp);
            const rect = inp.getBoundingClientRect();
            return {
                type: inp.type,
                placeholder: inp.placeholder,
                bg: cs.backgroundColor,
                border: cs.border,
                borderRadius: cs.borderRadius,
                rect: `${Math.round(rect.x)},${Math.round(rect.y)} ${Math.round(rect.width)}x${Math.round(rect.height)}`
            };
        });

        // 4. Overall page dimensions
        result.page = {
            width: document.documentElement.scrollWidth,
            height: document.documentElement.scrollHeight,
            viewport: `${window.innerWidth}x${window.innerHeight}`
        };

        return result;
    }""")

    print(json.dumps(analysis, indent=2, ensure_ascii=False))
    browser.close()
